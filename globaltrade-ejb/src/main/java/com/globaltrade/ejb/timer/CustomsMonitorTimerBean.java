package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.CustomsDeclaration;
import com.globaltrade.core.entity.ShipmentStatus;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Singleton
@Startup
public class CustomsMonitorTimerBean {

    private static final Logger logger = Logger.getLogger(CustomsMonitorTimerBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Schedule(hour = "*", minute = "*/5", persistent = false)
    public void monitorCustomsClearanceDelays() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusHours(48);

        List<CustomsDeclaration> delayedDeclarations = em.createQuery(
                "SELECT d FROM CustomsDeclaration d WHERE d.shipment.status = :status AND d.submissionDate < :thresholdDate",
                CustomsDeclaration.class)
                .setParameter("status", ShipmentStatus.AT_BORDER_PENDING_CLEARANCE)
                .setParameter("thresholdDate", thresholdDate)
                .getResultList();

        for (CustomsDeclaration declaration : delayedDeclarations) {
            logger.severe("CRITICAL ALERT: Shipment ID " + declaration.getShipment().getId() + 
                          " has been stuck at Customs for over 48 hours! " +
                          "Broker: " + declaration.getBrokerName() + 
                          ", Submitted: " + declaration.getSubmissionDate() + 
                          ". Imminent demurrage fees accruing!");
        }
    }
}
