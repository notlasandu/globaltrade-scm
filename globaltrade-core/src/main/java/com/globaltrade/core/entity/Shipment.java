package com.globaltrade.core.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "shipments")
public class Shipment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(unique = true)
    private String internalTrackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date estimatedDelivery;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getInternalTrackingNumber() { return internalTrackingNumber; }
    public void setInternalTrackingNumber(String internalTrackingNumber) { this.internalTrackingNumber = internalTrackingNumber; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public Date getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(Date estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
}
