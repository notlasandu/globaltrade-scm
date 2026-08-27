package com.globaltrade.ejb;

import com.globaltrade.ejb.exception.WMSSystemOutageException;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Singleton
@Startup
public class WarehouseManagementSystemSimulatorBean implements WMSSimulatorLocal, WMSSimulatorRemote {

    private static final Logger logger = Logger.getLogger(WarehouseManagementSystemSimulatorBean.class.getName());

    // Holds staging data for physical counts (SKU -> Count)
    private final ConcurrentHashMap<String, Integer> physicalStockCounts = new ConcurrentHashMap<>();
    
    private boolean isOffline = false;

    @Override
    public void reportPhysicalCount(String sku, int count) {
        logger.info("[WMS Simulator] Cycle count reported for " + sku + ": " + count);
        physicalStockCounts.put(sku, count);
    }

    @Override
    public Integer getPhysicalCount(String sku) {
        if (isOffline) {
            throw new WMSSystemOutageException("WMS API Connection Timed Out. System is currently offline.");
        }
        
        // Remove and return the physical count if it was reported
        return physicalStockCounts.remove(sku);
    }

    @Override
    public void simulateOutage(boolean offline) {
        this.isOffline = offline;
        logger.warning("[WMS Simulator] API Offline Status set to: " + offline);
    }
}
