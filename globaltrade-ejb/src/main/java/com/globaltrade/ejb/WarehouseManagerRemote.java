package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface WarehouseManagerRemote {
    List<Order> getPendingOrders();
    void packOrder(Long orderId);
}
