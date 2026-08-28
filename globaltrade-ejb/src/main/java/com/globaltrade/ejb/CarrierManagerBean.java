package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import com.globaltrade.ejb.service.ExceptionRecoveryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

@Stateless
@RolesAllowed("CARRIER")
public class CarrierManagerBean implements CarrierManagerRemote, CarrierManagerLocal {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private ExceptionRecoveryService recoveryService;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Order> getManifest() {
        TypedQuery<Order> query = em.createQuery(
                "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems LEFT JOIN FETCH o.orderingCustomer WHERE o.orderDeliveryStatus = :status", Order.class);
        query.setParameter("status", "SHIPPED");
        List<Order> orders = query.getResultList();

        for (Order order : orders) {
            List<OrderItem> strippedItems = new ArrayList<>(order.getOrderItems());
            order.setOrderItems(strippedItems);
        }

        return orders;
    }

    @Override
    public void updateTransitStatus(Long orderId, String eventCode) {
        if ("DELIVERED".equals(eventCode)) {
            Order order = em.find(Order.class, orderId);
            if (order != null && "SHIPPED".equals(order.getOrderDeliveryStatus())) {
                order.setOrderDeliveryStatus("DELIVERED");
                em.merge(order);
            }
        } else if ("BREAKDOWN".equals(eventCode)) {
            recoveryService.recoverFromCarrierFailure(orderId);
            
            throw new CarrierSystemOutageException("CRITICAL: Truck breakdown detected for Order ID " + orderId + ". Executing recovery protocols.");
        }
    }
}
