package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface WarehouseManagerLocal {
    List<Order> getPendingOrders();
    void packOrder(Long orderId);
}
