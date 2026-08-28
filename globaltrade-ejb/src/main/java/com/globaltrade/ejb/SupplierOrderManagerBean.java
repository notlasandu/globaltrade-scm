package com.globaltrade.ejb;

import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.ejb.exception.VendorSystemOutageException;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

@Stateless
@RolesAllowed("SYSTEM")
@Interceptors(AuditLoggingInterceptor.class)
public class SupplierOrderManagerBean implements SupplierOrderManagerLocal, SupplierOrderManagerRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void placeRestockOrder(Vendor vendor, String sku, int quantity) {
        
        if (!vendor.isEligible()) {
            throw new com.globaltrade.core.exception.SupplierNotEligibleException("CRITICAL: Vendor " + vendor.getName() + " is suspended due to poor performance evaluations.");
        }

        // Simulate a vendor API connection that occasionally times out (e.g., 5% chance) or deterministically for tests
        if (Math.random() < 0.05 || vendor.getName().startsWith("FAIL_VENDOR")) {
            throw new VendorSystemOutageException("CRITICAL: Vendor API Connection Timeout for supplier " + vendor.getName());
        }

        jakarta.persistence.TypedQuery<com.globaltrade.core.entity.Inventory> inventoryQuery = entityManager.createQuery(
                "SELECT i FROM Inventory i WHERE i.sku = :sku", com.globaltrade.core.entity.Inventory.class);
        inventoryQuery.setParameter("sku", sku);
        
        String productName = "Unknown Product";
        try {
            productName = inventoryQuery.getSingleResult().getProductName();
        } catch (jakarta.persistence.NoResultException e) {
            // Ignore and use default if for some reason the inventory item was deleted between polling and ordering
        }

        SupplierOrder newOrder = new SupplierOrder();
        newOrder.setVendor(vendor);
        newOrder.setSku(sku);
        newOrder.setProductName(productName);
        newOrder.setQuantity(quantity);
        newOrder.setStatus("REQUESTED");
        newOrder.setPlacementTimestamp(LocalDateTime.now());
        newOrder.setExpectedDeliveryDate(LocalDateTime.now().plusDays(5)); // Standard SLA

        entityManager.persist(newOrder);
    }
}
