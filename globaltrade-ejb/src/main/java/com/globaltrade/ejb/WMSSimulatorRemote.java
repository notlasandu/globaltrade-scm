package com.globaltrade.ejb;

import jakarta.ejb.Remote;

@Remote
public interface WMSSimulatorRemote {
    void reportPhysicalCount(String sku, int count);
    Integer getPhysicalCount(String sku);
    void simulateOutage(boolean offline);
}
