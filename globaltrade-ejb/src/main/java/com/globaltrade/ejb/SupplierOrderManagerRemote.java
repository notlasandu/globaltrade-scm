package com.globaltrade.ejb;

import com.globaltrade.core.entity.Vendor;
import jakarta.ejb.Remote;

@Remote
public interface SupplierOrderManagerRemote {
    void placeRestockOrder(Vendor vendor, String sku, int quantity);
}
