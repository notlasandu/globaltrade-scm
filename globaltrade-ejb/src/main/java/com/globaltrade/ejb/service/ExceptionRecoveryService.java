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
    public void recoverFromCarrierFailure(String trackingNumber) {
        java.util.List<Order> orders = em.createQuery("SELECT o FROM Order o WHERE o.trackingNumber = :tn", Order.class)
                .setParameter("tn", trackingNumber)
                .getResultList();
        if (!orders.isEmpty()) {
            Order order = orders.get(0);
            order.setOrderDeliveryStatus("DELAYED_TRANSIT_ISSUE");
            em.merge(order);
            return;
        }

        java.util.List<Shipment> shipments = em.createQuery("SELECT s FROM Shipment s WHERE s.trackingNumber = :tn", Shipment.class)
                .setParameter("tn", trackingNumber)
                .getResultList();
        if (!shipments.isEmpty()) {
            Shipment shipment = shipments.get(0);
            shipment.setStatus(ShipmentStatus.DELAYED_TRANSIT_ISSUE);
            em.merge(shipment);
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
