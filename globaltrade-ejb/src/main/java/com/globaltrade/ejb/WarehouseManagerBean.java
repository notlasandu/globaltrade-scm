package com.globaltrade.ejb;

import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.exception.InsufficientStockException;
import com.globaltrade.ejb.interceptor.AuditInterceptor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;

@Stateless
@Interceptors(AuditInterceptor.class)
@RolesAllowed({"WAREHOUSE_STAFF"})
public class WarehouseManagerBean implements WarehouseManagerLocal, WarehouseManagerRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Order> getPendingOrders() {
        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems LEFT JOIN FETCH o.orderingCustomer WHERE o.orderDeliveryStatus = 'PENDING'",
                Order.class
        );
        List<Order> orders = query.getResultList();

        for (Order order : orders) {
            List<OrderItem> strippedList = new ArrayList<>(order.getOrderItems());
            order.setOrderItems(strippedList);
        }

        return orders;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void packOrder(Long orderId) {
        Order order = entityManager.find(Order.class, orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order with ID " + orderId + " not found.");
        }

        if (!"PENDING".equals(order.getOrderDeliveryStatus())) {
            throw new IllegalArgumentException("Order is not in PENDING state.");
        }


        order.setOrderDeliveryStatus("PACKED");
        entityManager.merge(order);
    }
}
