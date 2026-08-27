package com.globaltrade.ejb;

import com.globaltrade.core.entity.Inventory;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface InventoryManagerRemote {
    List<Inventory> getAvailableProducts();
}
