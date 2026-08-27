package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface OrderManagerLocal {
    void placeOrder(Long customerId, List<OrderItem> items);
    List<Order> getOrdersForCustomer(Long customerId);
}
