package com.globaltrade.core.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "inventory")
public class Inventory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private int reorderThreshold;

    @Column(nullable = false)
    private int reorderQuantity;

    @ManyToOne(optional = true)
    @JoinColumn(name = "primaryVendorId")
    private Vendor primaryVendor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    public int getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(int reorderQuantity) { this.reorderQuantity = reorderQuantity; }

    public Vendor getPrimaryVendor() { return primaryVendor; }
    public void setPrimaryVendor(Vendor primaryVendor) { this.primaryVendor = primaryVendor; }
}
