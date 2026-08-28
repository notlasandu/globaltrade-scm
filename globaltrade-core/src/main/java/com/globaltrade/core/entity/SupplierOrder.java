package com.globaltrade.core.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_orders")
public class SupplierOrder implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false)
    private LocalDateTime placementTimestamp;

    @Column(nullable = false)
    private String status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendorId", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column
    private LocalDateTime expectedDeliveryDate;

    @Column
    private LocalDateTime receivedDate;

    @Column
    private Integer quantityAccepted;

    @Column
    private Boolean tradeDocumentationProvided;

    public SupplierOrder() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public LocalDateTime getPlacementTimestamp() { return placementTimestamp; }
    public void setPlacementTimestamp(LocalDateTime placementTimestamp) { this.placementTimestamp = placementTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDateTime getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public LocalDateTime getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDateTime receivedDate) { this.receivedDate = receivedDate; }

    public Integer getQuantityAccepted() { return quantityAccepted; }
    public void setQuantityAccepted(Integer quantityAccepted) { this.quantityAccepted = quantityAccepted; }

    public Boolean getTradeDocumentationProvided() { return tradeDocumentationProvided; }
    public void setTradeDocumentationProvided(Boolean tradeDocumentationProvided) { this.tradeDocumentationProvided = tradeDocumentationProvided; }
}
