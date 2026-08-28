package com.globaltrade.ejb;

import com.globaltrade.core.entity.CustomsDeclaration;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.core.exception.CustomsClearanceRejectedException;
import com.globaltrade.core.exception.GlobalTradeException;
import com.globaltrade.ejb.interceptor.CustomsComplianceInterceptor;
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
import java.util.UUID;

@ExtendWith(ArquillianExtension.class)
public class CustomsGatewayBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(CustomsGatewayBean.class, CustomsGatewayLocal.class, CustomsGatewayRemote.class,
                            CustomsClearanceRejectedException.class, com.globaltrade.core.exception.InvalidCustomsPaperworkException.class, GlobalTradeException.class, CustomsComplianceInterceptor.class,
                            CustomsGatewayTestWrapper.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private CustomsGatewayTestWrapper customsWrapper;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void processClearanceDecision_should_rollbackTransaction_onRejection() throws Exception {
        utx.begin();
        em.joinTransaction();

        Vendor vendor = new Vendor();
        vendor.setName(UUID.randomUUID().toString());
        vendor.setContactEmail(UUID.randomUUID().toString() + "@vendor.com");
        vendor.setPerformanceRating("A");
        em.persist(vendor);

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(UUID.randomUUID().toString());
        shipment.setStatus(ShipmentStatus.AT_BORDER_PENDING_CLEARANCE);
        shipment.setVendor(vendor);
        em.persist(shipment);
        
        utx.commit();

        Long shipmentId = shipment.getId();

        Assertions.assertThrows(Exception.class, () -> {
            customsWrapper.processClearanceDecision(shipmentId, false);
        });
        
        utx.begin();
        em.joinTransaction();
        Shipment retrievedShipment = em.find(Shipment.class, shipmentId);
        
        Assertions.assertEquals(ShipmentStatus.AT_BORDER_PENDING_CLEARANCE, retrievedShipment.getStatus(),
            "The EJB transaction should have rolled back completely on rejection.");
            
        em.remove(retrievedShipment);
        em.remove(em.find(Vendor.class, vendor.getId()));
        utx.commit();
    }
}
