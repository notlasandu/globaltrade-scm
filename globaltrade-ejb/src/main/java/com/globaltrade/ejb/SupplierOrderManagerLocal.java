package com.globaltrade.ejb;

import com.globaltrade.core.entity.Vendor;
import jakarta.ejb.Local;

@Local
public interface SupplierOrderManagerLocal {
    void placeRestockOrder(Vendor vendor, String sku, int quantity);
}
