package com.globaltrade.ejb;

import com.globaltrade.core.entity.Customer;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.core.entity.Inventory;
import com.globaltrade.ejb.exception.UnauthorizedOrderAccessException;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
@RolesAllowed("CUSTOMER")
@Interceptors(AuditLoggingInterceptor.class)
public class OrderManagerBean implements OrderManagerLocal, OrderManagerRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @Resource
    private SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void placeOrder(Long customerId, List<OrderItem> items) {
        String loggedInUsername = sessionContext.getCallerPrincipal().getName();

        Customer requestingCustomer = entityManager.find(Customer.class, customerId);

        if (requestingCustomer == null || !requestingCustomer.getLoginUsername().equals(loggedInUsername)) {
            throw new UnauthorizedOrderAccessException("Unauthorized access. Logged in as: '" + loggedInUsername + "', but requesting for: '" + (requestingCustomer != null ? requestingCustomer.getLoginUsername() : "null") + "'");
        }

        Order newOrder = new Order();
        newOrder.setOrderingCustomer(requestingCustomer);
        newOrder.setOrderPlacementTimestamp(LocalDateTime.now());
        newOrder.setOrderDeliveryStatus("PENDING");

        for (OrderItem item : items) {
            TypedQuery<Inventory> inventoryQuery = entityManager.createQuery(
                    "SELECT i FROM Inventory i WHERE i.sku = :sku", Inventory.class);
            inventoryQuery.setParameter("sku", item.getSku());
            
            Inventory inventory;
            try {
                inventory = inventoryQuery.getSingleResult();
            } catch (jakarta.persistence.NoResultException e) {
                throw new IllegalArgumentException("Product with SKU '" + item.getSku() + "' does not exist in inventory.");
            }
            
            item.setProductName(inventory.getProductName());
            
            if (inventory.getQuantity() < item.getQuantityRequested()) {
                throw new com.globaltrade.ejb.exception.InsufficientStockException("Insufficient stock for " + inventory.getProductName() + 
                    ". Requested: " + item.getQuantityRequested() + ", Available: " + inventory.getQuantity());
            }
            
            inventory.setQuantity(inventory.getQuantity() - item.getQuantityRequested());
            entityManager.merge(inventory);
            
            newOrder.addOrderItem(item);
        }

        entityManager.persist(newOrder);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Order> getOrdersForCustomer(Long customerId) {
        String loggedInUsername = sessionContext.getCallerPrincipal().getName();

        Customer requestingCustomer = entityManager.find(Customer.class, customerId);

        if (requestingCustomer == null || !requestingCustomer.getLoginUsername().equals(loggedInUsername)) {
            throw new UnauthorizedOrderAccessException("Unauthorized access to customer account data");
        }

        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems LEFT JOIN FETCH o.orderingCustomer WHERE o.orderingCustomer.customerId = :custId", Order.class);
        query.setParameter("custId", customerId);

        List<Order> orders = query.getResultList();
        
        for (Order o : orders) {
            if (o.getOrderItems() != null) {
                o.setOrderItems(new java.util.ArrayList<>(o.getOrderItems()));
            }
        }
        
        return orders;
    }
}
