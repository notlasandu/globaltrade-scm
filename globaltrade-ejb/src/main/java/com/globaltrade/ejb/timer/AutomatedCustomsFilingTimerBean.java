package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import com.globaltrade.core.exception.InvalidCustomsPaperworkException;
import com.globaltrade.ejb.CustomsGatewayLocal;
import com.globaltrade.ejb.service.ExceptionRecoveryService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

@Singleton
@RunAs("CUSTOMS_OFFICIAL")
@PermitAll
public class AutomatedCustomsFilingTimerBean {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private CustomsGatewayLocal customsGateway;

    @Inject
    private ExceptionRecoveryService recoveryService;

    @Schedule(minute = "*/10", hour = "*", persistent = true)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void processReadyForExportShipments() {
        TypedQuery<Shipment> query = em.createQuery(
                "SELECT s FROM Shipment s WHERE s.status = :status", Shipment.class);
        query.setParameter("status", ShipmentStatus.READY_FOR_EXPORT);
        List<Shipment> readyShipments = query.getResultList();

        for (Shipment shipment : readyShipments) {
            try {
                if (shipment.getTrackingNumber() != null && shipment.getTrackingNumber().contains("INVALID")) {
                    customsGateway.submitDeclaration(shipment.getId(), null, 0.0, "Automated Broker");
                } else {
                    customsGateway.submitDeclaration(shipment.getId(), "HS12345", 150.0, "Automated Broker");
                }
            } catch (InvalidCustomsPaperworkException e) {
                recoveryService.recoverCustomsPaperworkFailure(shipment.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
