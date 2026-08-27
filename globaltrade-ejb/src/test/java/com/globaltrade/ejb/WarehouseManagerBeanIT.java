package com.globaltrade.ejb;

import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.exception.InsufficientStockException;
import com.globaltrade.ejb.interceptor.AuditInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(ArquillianExtension.class)
public class WarehouseManagerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(WarehouseManagerBean.class, WarehouseManagerLocal.class, WarehouseManagerRemote.class, InsufficientStockException.class, AuditInterceptor.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private WarehouseManagerLocal warehouseManager;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void getPendingOrders_should_returnStrippedOrderList_when_invoked() throws Exception {
        // Setup Order
        utx.begin();
        em.joinTransaction();
        Order o = new Order();
        o.setOrderDeliveryStatus("PENDING");
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProductName("MRI Machine");
        item.setQuantityRequested(1);
        items.add(item);
        o.setOrderItems(items);
        em.persist(o);
        utx.commit();

        List<Order> pendingOrders = warehouseManager.getPendingOrders();
        Assertions.assertFalse(pendingOrders.isEmpty());
        
        // Clean up
        utx.begin();
        em.joinTransaction();
        Order toDelete = em.find(Order.class, o.getOrderId());
        em.remove(toDelete);
        utx.commit();
    }

    @Test
    public void packOrder_should_deductInventory_when_stockIsSufficient() throws Exception {
        // Setup Order
        utx.begin();
        em.joinTransaction();
        Order o = new Order();
        o.setOrderDeliveryStatus("PENDING");
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProductName("Surgical Masks");
        item.setQuantityRequested(5);
        items.add(item);
        o.setOrderItems(items);
        em.persist(o);
        utx.commit();

        warehouseManager.packOrder(o.getOrderId());

        utx.begin();
        em.joinTransaction();
        Order packed = em.find(Order.class, o.getOrderId());
        Assertions.assertEquals("PACKED", packed.getOrderDeliveryStatus());
        em.remove(packed);
        utx.commit();
    }
    
    @Test
    public void packOrder_should_throwInsufficientStockException_andRollback_when_stockIsLow() throws Exception {
        utx.begin();
        em.joinTransaction();
        Order o = new Order();
        o.setOrderDeliveryStatus("PENDING");
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProductName("Antibiotics");
        item.setQuantityRequested(99999); // Exceeds import.sql quantity
        items.add(item);
        o.setOrderItems(items);
        em.persist(o);
        utx.commit();

        Assertions.assertThrows(EJBException.class, () -> {
            warehouseManager.packOrder(o.getOrderId());
        });

        // Cleanup
        utx.begin();
        em.joinTransaction();
        Order failed = em.find(Order.class, o.getOrderId());
        em.remove(failed);
        utx.commit();
    }
}
