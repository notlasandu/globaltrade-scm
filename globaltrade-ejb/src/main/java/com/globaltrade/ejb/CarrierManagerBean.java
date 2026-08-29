package com.globaltrade.ejb;

import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import com.globaltrade.ejb.service.ExceptionRecoveryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
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
    public List<String> getManifest() {
        List<String> manifestStr = new ArrayList<>();
        
        TypedQuery<Order> query = em.createQuery(
                "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderingCustomer WHERE o.orderDeliveryStatus = :status", Order.class);
        query.setParameter("status", "PACKED");
        List<Order> orders = query.getResultList();
        for (Order order : orders) {
            manifestStr.add((order.getTrackingNumber() != null ? order.getTrackingNumber() : "N/A") + " | OUTBOUND | PACKED | " + order.getOrderingCustomer().getHospitalName());
        }

        TypedQuery<Shipment> shipmentQuery = em.createQuery(
                "SELECT DISTINCT s FROM Shipment s LEFT JOIN FETCH s.vendor WHERE s.status = :status", Shipment.class);
        shipmentQuery.setParameter("status", com.globaltrade.core.entity.ShipmentStatus.CLEARED_CUSTOMS);
        List<Shipment> shipments = shipmentQuery.getResultList();
        for (Shipment shipment : shipments) {
            String tracking = shipment.getInternalTrackingNumber() != null ? shipment.getInternalTrackingNumber() : shipment.getTrackingNumber();
            manifestStr.add(tracking + " | INBOUND | CLEARED_CUSTOMS | " + shipment.getVendor().getName());
        }

        return manifestStr;
    }

    @Override
    @PermitAll
    public String issueTrackingNumber(String prefix) {
        return prefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public void updateTransitStatus(String trackingNumber, String eventCode) {
        if ("BREAKDOWN".equals(eventCode)) {
            recoveryService.recoverFromCarrierFailure(trackingNumber);
            throw new CarrierSystemOutageException("CRITICAL: Truck breakdown detected for Tracking Number " + trackingNumber + ". Executing recovery protocols.");
        }

        if ("DELIVERED".equals(eventCode) || "IN_TRANSIT".equals(eventCode)) {
            String orderStatus = "DELIVERED".equals(eventCode) ? "DELIVERED" : "IN_TRANSIT";
            com.globaltrade.core.entity.ShipmentStatus shipStatus = "DELIVERED".equals(eventCode) ? com.globaltrade.core.entity.ShipmentStatus.DELIVERED : com.globaltrade.core.entity.ShipmentStatus.IN_TRANSIT;
            
            TypedQuery<Order> orderQuery = em.createQuery("SELECT o FROM Order o WHERE o.trackingNumber = :tn", Order.class);
            orderQuery.setParameter("tn", trackingNumber);
            List<Order> orders = orderQuery.getResultList();
            
            if (!orders.isEmpty()) {
                Order order = orders.get(0);
                order.setOrderDeliveryStatus(orderStatus);
                em.merge(order);
                return;
            }

            TypedQuery<Shipment> shipmentQuery = em.createQuery("SELECT s FROM Shipment s WHERE s.internalTrackingNumber = :tn OR s.trackingNumber = :tn", Shipment.class);
            shipmentQuery.setParameter("tn", trackingNumber);
            List<Shipment> shipments = shipmentQuery.getResultList();
            
            if (!shipments.isEmpty()) {
                Shipment shipment = shipments.get(0);
                shipment.setStatus(shipStatus);
                em.merge(shipment);
                
                if (shipStatus == com.globaltrade.core.entity.ShipmentStatus.DELIVERED) {
                    List<com.globaltrade.core.entity.SupplierOrder> supplierOrders = em.createQuery(
                            "SELECT so FROM SupplierOrder so WHERE so.shipment = :shipment", com.globaltrade.core.entity.SupplierOrder.class)
                            .setParameter("shipment", shipment)
                            .getResultList();
                    
                    for (com.globaltrade.core.entity.SupplierOrder so : supplierOrders) {
                        so.setStatus("RECEIVED");
                        so.setReceivedDate(java.time.LocalDateTime.now());
                        so.setQuantityAccepted(so.getQuantity());
                        em.merge(so);
                        
                        List<com.globaltrade.core.entity.Inventory> inventories = em.createQuery(
                                "SELECT i FROM Inventory i WHERE i.sku = :sku", com.globaltrade.core.entity.Inventory.class)
                                .setParameter("sku", so.getSku())
                                .getResultList();
                        
                        if (!inventories.isEmpty()) {
                            com.globaltrade.core.entity.Inventory inv = inventories.get(0);
                            inv.setQuantity(inv.getQuantity() + so.getQuantity());
                            em.merge(inv);
                        }
                    }
                }
            }
        }
    }
}
