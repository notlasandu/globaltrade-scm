package com.globaltrade.ejb;

import com.globaltrade.core.entity.Inventory;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface InventoryManagerLocal {
    List<Inventory> getAvailableProducts();
}
