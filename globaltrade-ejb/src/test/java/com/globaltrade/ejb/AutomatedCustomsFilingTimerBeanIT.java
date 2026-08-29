package com.globaltrade.ejb;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.core.exception.GlobalTradeException;
import com.globaltrade.core.exception.InvalidCustomsPaperworkException;
import com.globaltrade.ejb.interceptor.CustomsComplianceInterceptor;
import com.globaltrade.ejb.service.ExceptionRecoveryService;
import com.globaltrade.ejb.timer.AutomatedCustomsFilingTimerBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import java.util.UUID;

@ExtendWith(ArquillianExtension.class)
public class AutomatedCustomsFilingTimerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test-timer.jar")
                .addClasses(AutomatedCustomsFilingTimerBean.class,
                            CustomsGatewayBean.class, CustomsGatewayLocal.class, CustomsGatewayRemote.class,
                            InvalidCustomsPaperworkException.class, com.globaltrade.core.exception.CustomsClearanceRejectedException.class, GlobalTradeException.class, CustomsComplianceInterceptor.class,
                            ExceptionRecoveryService.class, CarrierManagerBean.class, CarrierManagerLocal.class, CarrierManagerRemote.class, com.globaltrade.ejb.exception.CarrierSystemOutageException.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private AutomatedCustomsFilingTimerBean automatedFilingTimer;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void processReadyForExportShipments_should_recoverFromInvalidPaperwork() throws Exception {
        utx.begin();
        em.joinTransaction();

        Vendor vendor = new Vendor();
        vendor.setName(UUID.randomUUID().toString());
        vendor.setContactEmail(UUID.randomUUID().toString() + "@vendor.com");
        vendor.setPerformanceRating("F");
        em.persist(vendor);

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("INVALID-" + UUID.randomUUID().toString());
        shipment.setStatus(ShipmentStatus.READY_FOR_EXPORT);
        shipment.setVendor(vendor);
        em.persist(shipment);
        
        utx.commit();

        Long shipmentId = shipment.getId();

        automatedFilingTimer.processReadyForExportShipments();
        
        utx.begin();
        em.joinTransaction();
        Shipment retrievedShipment = em.find(Shipment.class, shipmentId);
        
        Assertions.assertEquals(ShipmentStatus.CUSTOMS_PAPERWORK_REJECTED, retrievedShipment.getStatus(),
            "The EJB transaction should have used ExceptionRecoveryService (REQUIRES_NEW) to save the failed state.");
            
        em.remove(retrievedShipment);
        em.remove(em.find(Vendor.class, vendor.getId()));
        utx.commit();
    }
}
