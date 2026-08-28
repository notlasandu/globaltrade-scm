package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.Inventory;
import com.globaltrade.ejb.SupplierOrderManagerLocal;
import com.globaltrade.ejb.WMSSimulatorLocal;
import com.globaltrade.ejb.exception.VendorSystemOutageException;
import com.globaltrade.ejb.exception.WMSSystemOutageException;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;

import java.util.List;
import java.util.logging.Logger;

@Singleton
@Startup
@RunAs("SYSTEM")
@PermitAll
public class InventoryReplenishmentPollerBean {

    private static final Logger logger = Logger.getLogger(InventoryReplenishmentPollerBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @Inject
    private SupplierOrderManagerLocal supplierOrderManager;

    @Inject
    private WMSSimulatorLocal wmsSimulator;

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    public void pollInventoryLevels() {
        logger.info("Starting automated inventory replenishment poll...");

        logger.info("Reconciling physical counts from WMS...");
        TypedQuery<Inventory> allItemsQuery = entityManager.createQuery("SELECT i FROM Inventory i", Inventory.class);
        List<Inventory> allItems = allItemsQuery.getResultList();

        for (Inventory item : allItems) {
            try {
                Integer physicalCount = wmsSimulator.getPhysicalCount(item.getSku());
                if (physicalCount != null && physicalCount != item.getQuantity()) {
                    logger.warning("Discrepancy detected for SKU " + item.getSku() + "! DB: " + item.getQuantity() + " | WMS: " + physicalCount + ". Syncing database...");
                    item.setQuantity(physicalCount);
                    entityManager.merge(item);
                }
            } catch (WMSSystemOutageException e) {
                logger.warning("WMS connection failed for SKU: " + item.getSku() + ". Skipping reconciliation. " + e.getMessage());
            }
        }

        logger.info("Checking for low stock...");
        TypedQuery<Inventory> query = entityManager.createQuery(
                "SELECT i FROM Inventory i WHERE i.quantity < i.reorderThreshold", Inventory.class);
        List<Inventory> lowStockItems = query.getResultList();

        for (Inventory item : lowStockItems) {
            
            TypedQuery<Long> pendingOrderQuery = entityManager.createQuery(
                    "SELECT COUNT(s) FROM SupplierOrder s WHERE s.sku = :sku AND s.status IN ('REQUESTED', 'SHIPPED')", Long.class);
            pendingOrderQuery.setParameter("sku", item.getSku());

            if (pendingOrderQuery.getSingleResult() == 0) {
                logger.info("Stock low for SKU: " + item.getSku() + ". Available: " + item.getQuantity() 
                            + ", Threshold: " + item.getReorderThreshold() + ". Triggering reorder.");

                try {
                    supplierOrderManager.placeRestockOrder(item.getPrimaryVendor(), item.getSku(), item.getReorderQuantity());
                    logger.info("Successfully placed restock order for SKU: " + item.getSku());
                } catch (VendorSystemOutageException e) {
                    logger.warning("Failed to place order for SKU: " + item.getSku() + " due to Vendor Outage. " + e.getMessage());
                } catch (Exception e) {
                    logger.severe("Unexpected error while placing order for SKU: " + item.getSku() + ". " + e.getMessage());
                }
            } else {
                logger.info("Stock low for SKU: " + item.getSku() + ", but a restock order is already pending.");
            }
        }
    }
}
