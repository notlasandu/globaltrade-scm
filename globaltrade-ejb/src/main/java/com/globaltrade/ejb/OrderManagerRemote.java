package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import jakarta.ejb.Remote;

import java.util.List;

@Remote
public interface OrderManagerRemote {
    void placeOrder(Long customerId, List<OrderItem> items);
    List<Order> getOrdersForCustomer(Long customerId);
}
