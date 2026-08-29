package com.globaltrade.ejb.service;

import com.globaltrade.core.entity.AuditLog;
import com.globaltrade.core.entity.Shipment;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class EngineDashboardBean implements EngineDashboardLocal, EngineDashboardRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EngineDashboardDTO getDashboardData() {
        EngineDashboardDTO dto = new EngineDashboardDTO();

        // 1. Fetch Database Counts
        Long totalOrders = em.createQuery("SELECT COUNT(o) FROM Order o", Long.class).getSingleResult();
        Long totalShipments = em.createQuery("SELECT COUNT(s) FROM Shipment s", Long.class).getSingleResult();
        Long totalInventory = em.createQuery("SELECT COUNT(i) FROM Inventory i", Long.class).getSingleResult();
        Long totalVendors = em.createQuery("SELECT COUNT(v) FROM Vendor v", Long.class).getSingleResult();

        dto.setTotalOrders(totalOrders != null ? totalOrders : 0);
        dto.setTotalShipments(totalShipments != null ? totalShipments : 0);
        dto.setTotalInventoryItems(totalInventory != null ? totalInventory : 0);
        dto.setTotalVendors(totalVendors != null ? totalVendors : 0);

        // 2. Fetch Recent Outbound Orders (Warehouse -> Hospital)
        List<com.globaltrade.core.entity.Order> outbound = em.createQuery(
                "SELECT o FROM Order o LEFT JOIN FETCH o.orderingCustomer ORDER BY o.orderId DESC", com.globaltrade.core.entity.Order.class)
                .setMaxResults(10)
                .getResultList();
        dto.setRecentOutboundOrders(outbound);

        // 3. Fetch Recent Inbound Orders (Vendor -> Warehouse)
        List<com.globaltrade.core.entity.SupplierOrder> inbound = em.createQuery(
                "SELECT so FROM SupplierOrder so LEFT JOIN FETCH so.vendor LEFT JOIN FETCH so.shipment ORDER BY so.orderId DESC", com.globaltrade.core.entity.SupplierOrder.class)
                .setMaxResults(10)
                .getResultList();
        dto.setRecentInboundOrders(inbound);
        
        // 4. Fetch Stock Counts
        List<com.globaltrade.core.entity.Inventory> stock = em.createQuery(
                "SELECT i FROM Inventory i ORDER BY i.sku ASC", com.globaltrade.core.entity.Inventory.class)
                .setMaxResults(50)
                .getResultList();
        dto.setStockCounts(stock);

        // 5. Fetch Pending Restock SKUs
        List<String> pendingSkusList = em.createQuery(
                "SELECT DISTINCT so.sku FROM SupplierOrder so WHERE so.status NOT IN ('RECEIVED', 'DELIVERED')", String.class)
                .getResultList();
        dto.setPendingRestockSkus(new java.util.HashSet<>(pendingSkusList));

        // 3. Fetch Recent Exceptions (from AuditLog)
        // Since we want errors or exceptions, we look for 'ERROR' or 'EXCEPTION' in action or details
        List<AuditLog> recentExceptions = em.createQuery(
                "SELECT a FROM AuditLog a WHERE a.action LIKE '%ERROR%' OR a.action LIKE '%EXCEPTION%' OR a.details LIKE '%Exception%' ORDER BY a.timestamp DESC", AuditLog.class)
                .setMaxResults(10)
                .getResultList();
        
        // If we don't have many exceptions, just fetch the most recent audit logs so the dashboard isn't empty
        if (recentExceptions.isEmpty()) {
            recentExceptions = em.createQuery("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC", AuditLog.class)
                .setMaxResults(10)
                .getResultList();
        }
        
        dto.setRecentExceptions(recentExceptions);

        return dto;
    }
}
