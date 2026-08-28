package com.globaltrade.ejb;

import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

        // RMI Safety: Strip any potential Hibernate PersistentBags if there were collections (none currently, but standard practice)
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
    public void fulfillOrder(Long vendorId, Long orderId, boolean tradeDocsProvided) {
        SupplierOrder order = em.find(SupplierOrder.class, orderId);
        if (order == null || !order.getVendor().getId().equals(vendorId)) {
            throw new IllegalArgumentException("Order not found or does not belong to vendor.");
        }
        if (!"REQUESTED".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order must be in REQUESTED state to fulfill.");
        }
        order.setStatus("SHIPPED");
        order.setTradeDocumentationProvided(tradeDocsProvided);
        em.merge(order);
    }
}
