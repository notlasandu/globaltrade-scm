package com.globaltrade.ejb;

import com.globaltrade.core.entity.Customer;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import com.globaltrade.ejb.service.ExceptionRecoveryService;
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

@ExtendWith(ArquillianExtension.class)
public class CarrierManagerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(CarrierManagerBean.class, CarrierManagerLocal.class, CarrierManagerRemote.class, ExceptionRecoveryService.class, CarrierSystemOutageException.class, CarrierManagerTestWrapper.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private CarrierManagerTestWrapper carrierManagerWrapper;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void updateTransitStatus_should_throwExceptionAndExecuteRecovery_onBreakdown() throws Exception {
        utx.begin();
        em.joinTransaction();
        
        Customer customer = new Customer();
        customer.setHospitalName("Carrier Hosp");
        customer.setContactEmail(java.util.UUID.randomUUID().toString() + "@hospital.com");
        customer.setLoginUsername(java.util.UUID.randomUUID().toString());
        customer.setLoginPasswordHash("hash123");
        em.persist(customer);

        Order o = new Order();
        o.setOrderDeliveryStatus("SHIPPED");
        o.setOrderPlacementTimestamp(java.time.LocalDateTime.now());
        o.setOrderingCustomer(customer);
        String testTrackingNumber = "TRK-" + java.util.UUID.randomUUID().toString();
        o.setTrackingNumber(testTrackingNumber);

        OrderItem item = new OrderItem();
        item.setSku(java.util.UUID.randomUUID().toString());
        item.setProductName("Test Package");
        item.setQuantityRequested(1);
        o.addOrderItem(item);
        em.persist(o);
        utx.commit();

        Long orderId = o.getOrderId();

        Assertions.assertThrows(CarrierSystemOutageException.class, () -> {
            carrierManagerWrapper.updateTransitStatus(testTrackingNumber, "BREAKDOWN");
        });

        utx.begin();
        em.joinTransaction();
        Order recoveredOrder = em.find(Order.class, orderId);
        
        Assertions.assertEquals("DELAYED_TRANSIT_ISSUE", recoveredOrder.getOrderDeliveryStatus(), 
            "The EJB REQUIRES_NEW transaction failed to save the delayed status!");

        em.remove(recoveredOrder);
        utx.commit();
    }
}
