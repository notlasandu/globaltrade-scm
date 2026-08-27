package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.annotation.security.RunAs;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.util.List;

@Stateless
@RunAs("WAREHOUSE_STAFF")
@PermitAll
public class WarehouseManagerTestWrapper {
    @EJB
    private WarehouseManagerLocal warehouseManager;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Order> getPendingOrders() {
        return warehouseManager.getPendingOrders();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void packOrder(Long orderId) {
        warehouseManager.packOrder(orderId);
    }
}
