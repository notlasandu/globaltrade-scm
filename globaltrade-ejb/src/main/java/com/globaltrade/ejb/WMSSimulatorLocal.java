package com.globaltrade.ejb;

import jakarta.ejb.Local;

@Local
public interface WMSSimulatorLocal {
    void reportPhysicalCount(String sku, int count);
    Integer getPhysicalCount(String sku);
    void simulateOutage(boolean offline);
}
