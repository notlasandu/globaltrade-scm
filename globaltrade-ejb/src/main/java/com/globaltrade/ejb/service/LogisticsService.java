package com.globaltrade.ejb.service;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import com.globaltrade.core.exception.GlobalTradeException;
import com.globaltrade.ejb.interceptor.AuditInterceptor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

@Stateless
@Interceptors(AuditInterceptor.class)
public class LogisticsService {
    private static final Logger logger = Logger.getLogger(LogisticsService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @RolesAllowed({"COORDINATOR", "VENDOR_REP"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateShipmentStatus(Long shipmentId, ShipmentStatus status) throws GlobalTradeException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new GlobalTradeException("Shipment not found with ID: " + shipmentId);
        }
        
        shipment.setStatus(status);
        em.merge(shipment);
        logger.info("Updated shipment " + shipmentId + " to status " + status);
    }
}
