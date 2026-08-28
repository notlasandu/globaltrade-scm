package com.globaltrade.ejb.service;

import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ExceptionRecoveryService {

    @PersistenceContext
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void recoverFromCarrierFailure(Long orderId) {
        Order order = em.find(Order.class, orderId);
        if (order != null) {
            order.setOrderDeliveryStatus("DELAYED_TRANSIT_ISSUE");
            em.merge(order);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void recoverCustomsPaperworkFailure(Long shipmentId) {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment != null) {
            shipment.setStatus(ShipmentStatus.CUSTOMS_PAPERWORK_REJECTED);
            em.merge(shipment);
        }
    }
}
