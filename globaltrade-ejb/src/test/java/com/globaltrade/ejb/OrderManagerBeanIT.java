package com.globaltrade.ejb;

import com.globaltrade.ejb.exception.UnauthorizedOrderAccessException;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.globaltrade.ejb.exception.InsufficientStockException;
import com.globaltrade.core.entity.Customer;
import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.OrderItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;
import java.util.Collections;

@ExtendWith(ArquillianExtension.class)
public class OrderManagerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(OrderManagerBean.class, OrderManagerLocal.class, OrderManagerRemote.class, 
                            UnauthorizedOrderAccessException.class, AuditLoggingInterceptor.class,
                            InsufficientStockException.class, OrderManagerTestWrapper.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private OrderManagerLocal orderManager;

    @EJB
    private OrderManagerTestWrapper wrapper;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void getOrdersForCustomer_should_throwException_when_noUserSessionIsActive() {
        Assertions.assertThrows(EJBException.class, () -> {
            orderManager.getOrdersForCustomer(1L);
        });
    }

    @Test
    public void should_Lookup_RemoteInterface_viaJNDI() throws Exception {
        Context context = new InitialContext();
        
        String jndiName = "java:module/OrderManagerBean!com.globaltrade.ejb.OrderManagerRemote";
        
        OrderManagerRemote remoteManager = (OrderManagerRemote) context.lookup(jndiName);
        
        Assertions.assertNotNull(remoteManager, "The Remote Interface lookup should not be null.");
    }

    @Test
    public void placeOrder_should_throwInsufficientStockException_andRollback_when_stockIsLow() throws Exception {
        utx.begin();
        em.joinTransaction();
        Customer c = new Customer();
        c.setHospitalName("Order Hosp 3");
        c.setContactEmail(java.util.UUID.randomUUID().toString() + "@test.com");
        c.setLoginUsername("CUSTOMER");
        c.setLoginPasswordHash("hash");
        em.persist(c);
        
        String sku = "Anti_" + java.util.UUID.randomUUID().toString();
        OrderItem item = new OrderItem();
        item.setSku(sku);
        item.setProductName("Test Product");
        item.setQuantityRequested(99999);
        
        Inventory inv = new Inventory();
        inv.setSku(sku);
        inv.setProductName("Test Product");
        inv.setQuantity(500);
        inv.setLocation("Test Location");
        inv.setReorderThreshold(10);
        inv.setReorderQuantity(50);
        em.persist(inv);
        utx.commit();

        Assertions.assertThrows(InsufficientStockException.class, () -> {
            wrapper.placeOrder(c.getCustomerId(), Collections.singletonList(item));
        });

        utx.begin();
        em.joinTransaction();
        em.remove(em.merge(c));
        em.remove(em.merge(inv));
        utx.commit();
    }
}
