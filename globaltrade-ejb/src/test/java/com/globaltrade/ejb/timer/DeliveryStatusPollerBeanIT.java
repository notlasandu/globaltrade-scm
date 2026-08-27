package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.Customer;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.CarrierTrackingSimulatorBean;
import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

@ExtendWith(ArquillianExtension.class)
public class DeliveryStatusPollerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(DeliveryStatusPollerBean.class, CarrierTrackingSimulatorBean.class,
                        CarrierSystemOutageException.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @Inject
    private DeliveryStatusPollerBean pollerBean;

    @Test
    public void pollDeliveryStatuses_should_executeWithoutCrashing_when_invoked() {
        Assertions.assertDoesNotThrow(() -> {
            pollerBean.pollDeliveryStatuses();
        });
    }

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void pollDeliveryStatuses_should_transitionPackedToShipped() throws Exception {
        utx.begin();
        em.joinTransaction();
        Customer c = new Customer();
        c.setHospitalName("Poller Hosp");
        c.setContactEmail("poller@test.com");
        c.setLoginUsername("polleruser");
        c.setLoginPasswordHash("hash");
        em.persist(c);
        Order o = new Order();
        o.setOrderingCustomer(c);
        o.setOrderPlacementTimestamp(java.time.LocalDateTime.now());
        o.setOrderDeliveryStatus("PACKED");
        em.persist(o);
        utx.commit();

        pollerBean.pollDeliveryStatuses();

        utx.begin();
        em.joinTransaction();
        Order updated = em.find(Order.class, o.getOrderId());
        Assertions.assertEquals("SHIPPED", updated.getOrderDeliveryStatus());
        em.remove(updated);
        utx.commit();
    }
}
