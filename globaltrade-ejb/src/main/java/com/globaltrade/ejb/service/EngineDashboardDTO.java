package com.globaltrade.ejb.service;

import com.globaltrade.core.entity.AuditLog;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Inventory;
import java.util.List;

public class EngineDashboardDTO {
    
    private long totalOrders;
    private long totalShipments;
    private long totalInventoryItems;
    private long totalVendors;
    
    private List<Shipment> recentShipments;
    private List<AuditLog> recentExceptions;
    
    private List<Order> recentOutboundOrders;
    private List<SupplierOrder> recentInboundOrders;
    private List<Inventory> stockCounts;
    private java.util.Set<String> pendingRestockSkus;
    
    public EngineDashboardDTO() {}

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalShipments() {
        return totalShipments;
    }

    public void setTotalShipments(long totalShipments) {
        this.totalShipments = totalShipments;
    }

    public long getTotalInventoryItems() {
        return totalInventoryItems;
    }

    public void setTotalInventoryItems(long totalInventoryItems) {
        this.totalInventoryItems = totalInventoryItems;
    }

    public long getTotalVendors() {
        return totalVendors;
    }

    public void setTotalVendors(long totalVendors) {
        this.totalVendors = totalVendors;
    }

    public List<Shipment> getRecentShipments() {
        return recentShipments;
    }

    public void setRecentShipments(List<Shipment> recentShipments) {
        this.recentShipments = recentShipments;
    }

    public List<AuditLog> getRecentExceptions() {
        return recentExceptions;
    }

    public void setRecentExceptions(List<AuditLog> recentExceptions) {
        this.recentExceptions = recentExceptions;
    }

    public List<Order> getRecentOutboundOrders() {
        return recentOutboundOrders;
    }

    public void setRecentOutboundOrders(List<Order> recentOutboundOrders) {
        this.recentOutboundOrders = recentOutboundOrders;
    }

    public List<SupplierOrder> getRecentInboundOrders() {
        return recentInboundOrders;
    }

    public void setRecentInboundOrders(List<SupplierOrder> recentInboundOrders) {
        this.recentInboundOrders = recentInboundOrders;
    }

    public List<Inventory> getStockCounts() {
        return stockCounts;
    }

    public void setStockCounts(List<Inventory> stockCounts) {
        this.stockCounts = stockCounts;
    }

    public java.util.Set<String> getPendingRestockSkus() {
        return pendingRestockSkus;
    }

    public void setPendingRestockSkus(java.util.Set<String> pendingRestockSkus) {
        this.pendingRestockSkus = pendingRestockSkus;
    }
}
