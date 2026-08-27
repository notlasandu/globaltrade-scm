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
}
