package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.Order;
import com.globaltrade.ejb.CarrierTrackingSimulatorBean;
import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

@Singleton
@Startup
public class DeliveryStatusPollerBean {

    private static final Logger pollerLogger = Logger.getLogger("DeliveryStatusPoller");

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @Inject
    private CarrierTrackingSimulatorBean carrierTrackingSimulator;

    @Schedule(hour = "*", minute = "*/15", persistent = true)
    public void pollDeliveryStatuses() {
        pollerLogger.info("Starting automated delivery status poll...");

        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT o FROM Order o WHERE o.orderDeliveryStatus = 'PACKED' OR o.orderDeliveryStatus = 'SHIPPED'", Order.class);
        List<Order> activeOrders = query.getResultList();

        for (Order currentOrder : activeOrders) {
            try {
                if ("PACKED".equals(currentOrder.getOrderDeliveryStatus())) {
                    pollerLogger.info("Allocating carrier for packed order " + currentOrder.getOrderId());
                    currentOrder.setOrderDeliveryStatus("SHIPPED");
                    entityManager.merge(currentOrder);
                } else if ("SHIPPED".equals(currentOrder.getOrderDeliveryStatus())) {
                    String updatedStatus = carrierTrackingSimulator.checkShipmentStatus(currentOrder.getOrderId());
                    currentOrder.setOrderDeliveryStatus(updatedStatus);
                    entityManager.merge(currentOrder);
                }
            } catch (CarrierSystemOutageException outageException) {
                pollerLogger.warning("Carrier system outage detected while checking order " + currentOrder.getOrderId() + ". Skipping until next poll.");
            }
        }
    }
}
