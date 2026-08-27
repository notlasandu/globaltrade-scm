package com.globaltrade.ejb;

import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.exception.InsufficientStockException;
import com.globaltrade.ejb.interceptor.AuditInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import com.globaltrade.core.entity.Customer;

@ExtendWith(ArquillianExtension.class)
public class WarehouseManagerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(WarehouseManagerBean.class, WarehouseManagerLocal.class, WarehouseManagerRemote.class, InsufficientStockException.class, AuditInterceptor.class, WarehouseManagerTestWrapper.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private WarehouseManagerTestWrapper warehouseManagerWrapper;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void getPendingOrders_should_returnStrippedOrderList_when_invoked() throws Exception {
        // Setup Order
        utx.begin();
        em.joinTransaction();
        Customer c = new Customer();
        c.setHospitalName("WH Hosp 1");
        c.setContactEmail(java.util.UUID.randomUUID().toString() + "@test.com");
        c.setLoginUsername(java.util.UUID.randomUUID().toString());
        c.setLoginPasswordHash("hash");
        em.persist(c);
        Order o = new Order();
        o.setOrderingCustomer(c);
        o.setOrderPlacementTimestamp(java.time.LocalDateTime.now());
        o.setOrderDeliveryStatus("PENDING");
        String productName = "MRI_" + java.util.UUID.randomUUID().toString();
        OrderItem item = new OrderItem();
        item.setProductName(productName);
        item.setQuantityRequested(1);
        Inventory inv = new Inventory();
        inv.setSku(productName);
        inv.setQuantity(50);
        inv.setLocation("Test Location");
        em.persist(inv);
        
        o.addOrderItem(item);
        em.persist(o);
        utx.commit();

        List<Order> pendingOrders = warehouseManagerWrapper.getPendingOrders();
        Assertions.assertFalse(pendingOrders.isEmpty());
        
        // Clean up
        utx.begin();
        em.joinTransaction();
        Order toDelete = em.find(Order.class, o.getOrderId());
        em.remove(toDelete);
        utx.commit();
    }

    @Test
    public void packOrder_should_deductInventory_when_stockIsSufficient() throws Exception {
        // Setup Order
        utx.begin();
        em.joinTransaction();
        Customer c = new Customer();
        c.setHospitalName("WH Hosp 2");
        c.setContactEmail(java.util.UUID.randomUUID().toString() + "@test.com");
        c.setLoginUsername(java.util.UUID.randomUUID().toString());
        c.setLoginPasswordHash("hash");
        em.persist(c);
        Order o = new Order();
        o.setOrderingCustomer(c);
        o.setOrderPlacementTimestamp(java.time.LocalDateTime.now());
        o.setOrderDeliveryStatus("PENDING");
        String productName = "Masks_" + java.util.UUID.randomUUID().toString();
        OrderItem item = new OrderItem();
        item.setProductName(productName);
        item.setQuantityRequested(5);
        Inventory inv = new Inventory();
        inv.setSku(productName);
        inv.setQuantity(500);
        inv.setLocation("Test Location");
        em.persist(inv);

        o.addOrderItem(item);
        em.persist(o);
        utx.commit();

        warehouseManagerWrapper.packOrder(o.getOrderId());

        utx.begin();
        em.joinTransaction();
        Order packed = em.find(Order.class, o.getOrderId());
        Assertions.assertEquals("PACKED", packed.getOrderDeliveryStatus());
        em.remove(packed);
        utx.commit();
    }
    
    @Test
    public void packOrder_should_throwInsufficientStockException_andRollback_when_stockIsLow() throws Exception {
        utx.begin();
        em.joinTransaction();
        Customer c = new Customer();
        c.setHospitalName("WH Hosp 3");
        c.setContactEmail(java.util.UUID.randomUUID().toString() + "@test.com");
        c.setLoginUsername(java.util.UUID.randomUUID().toString());
        c.setLoginPasswordHash("hash");
        em.persist(c);
        Order o = new Order();
        o.setOrderingCustomer(c);
        o.setOrderPlacementTimestamp(java.time.LocalDateTime.now());
        o.setOrderDeliveryStatus("PENDING");
        String productName = "Anti_" + java.util.UUID.randomUUID().toString();
        OrderItem item = new OrderItem();
        item.setProductName(productName);
        item.setQuantityRequested(99999);
        Inventory inv = new Inventory();
        inv.setSku(productName);
        inv.setQuantity(500); // Has 500, we request 99999
        inv.setLocation("Test Location");
        em.persist(inv);

        o.addOrderItem(item);
        em.persist(o);
        utx.commit();

        Assertions.assertThrows(InsufficientStockException.class, () -> {
            warehouseManagerWrapper.packOrder(o.getOrderId());
        });

        // Cleanup
        utx.begin();
        em.joinTransaction();
        Order failed = em.find(Order.class, o.getOrderId());
        em.remove(failed);
        utx.commit();
    }
}
