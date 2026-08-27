package com.globaltrade.ejb;

import com.globaltrade.core.entity.OrderItem;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;

@Stateless
@RunAs("CUSTOMER")
@PermitAll
public class OrderManagerTestWrapper {

    @EJB
    private OrderManagerLocal orderManager;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void placeOrder(Long customerId, List<OrderItem> items) {
        orderManager.placeOrder(customerId, items);
    }
}
