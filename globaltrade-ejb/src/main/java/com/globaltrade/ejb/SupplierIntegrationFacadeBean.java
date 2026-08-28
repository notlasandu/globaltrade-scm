package com.globaltrade.ejb;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import jakarta.annotation.security.RolesAllowed;
import jakarta.interceptor.Interceptors;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

@Stateless
@RolesAllowed({"VENDOR_REP", "SystemIntegration"})
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SupplierIntegrationFacadeBean implements SupplierIntegrationFacadeRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    public String ping() {
        return "PONG: Authenticated successfully!";
    }

    @Override
    public List<SupplierOrder> getActiveOrdersForVendor(Long vendorId) {
        List<SupplierOrder> orders = em.createQuery(
                "SELECT s FROM SupplierOrder s LEFT JOIN FETCH s.vendor WHERE s.vendor.id = :vendorId AND s.status = 'REQUESTED'",
                SupplierOrder.class)
                .setParameter("vendorId", vendorId)
                .getResultList();

        return new ArrayList<>(orders);
    }

    @Override
    public List<SupplierEvaluation> getVendorEvaluations(Long vendorId) {
        List<SupplierEvaluation> evaluations = em.createQuery(
                "SELECT e FROM SupplierEvaluation e LEFT JOIN FETCH e.vendor WHERE e.vendor.id = :vendorId ORDER BY e.evaluationDate DESC",
                SupplierEvaluation.class)
                .setParameter("vendorId", vendorId)
                .getResultList();

        return new ArrayList<>(evaluations);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Interceptors(LogisticsAuditInterceptor.class)
    public void fulfillOrder(Long vendorId, Long orderId, boolean tradeDocsProvided, String trackingNumber) {
        SupplierOrder order = em.find(SupplierOrder.class, orderId);
        if (order == null || !order.getVendor().getId().equals(vendorId)) {
            throw new IllegalArgumentException("Order not found or does not belong to vendor.");
        }
        if (!"REQUESTED".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order must be in REQUESTED state to fulfill.");
        }
        
        Shipment shipment = null;
        try {
            TypedQuery<Shipment> query = em.createQuery("SELECT s FROM Shipment s WHERE s.trackingNumber = :trackingNumber", Shipment.class);
            query.setParameter("trackingNumber", trackingNumber);
            shipment = query.getSingleResult();
        } catch (NoResultException e) {
            shipment = new Shipment();
            shipment.setTrackingNumber(trackingNumber);
            shipment.setVendor(order.getVendor());
            shipment.setStatus(ShipmentStatus.READY_FOR_EXPORT);
            em.persist(shipment);
        }
        
        order.setShipment(shipment);
        order.setStatus("SHIPPED");
        order.setTradeDocumentationProvided(tradeDocsProvided);
        em.merge(order);
    }
}
