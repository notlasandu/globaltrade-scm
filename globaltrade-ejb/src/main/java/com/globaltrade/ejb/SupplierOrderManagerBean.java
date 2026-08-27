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
        
        // Simulate a vendor API connection that occasionally times out (e.g., 5% chance) or deterministically for tests
        if (Math.random() < 0.05 || vendor.getName().startsWith("FAIL_VENDOR")) {
            throw new VendorSystemOutageException("CRITICAL: Vendor API Connection Timeout for supplier " + vendor.getName());
        }

        SupplierOrder newOrder = new SupplierOrder();
        newOrder.setVendor(vendor);
        newOrder.setSku(sku);
        newOrder.setQuantity(quantity);
        newOrder.setStatus("REQUESTED");
        newOrder.setPlacementTimestamp(LocalDateTime.now());

        entityManager.persist(newOrder);
    }
}
