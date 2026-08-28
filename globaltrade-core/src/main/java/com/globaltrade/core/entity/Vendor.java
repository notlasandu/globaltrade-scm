package com.globaltrade.core.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "vendors")
public class Vendor implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private String contactEmail;
    
    @Column(nullable = false)
    private String performanceRating;

    @Column(nullable = false)
    private boolean eligible = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getPerformanceRating() { return performanceRating; }
    public void setPerformanceRating(String performanceRating) { this.performanceRating = performanceRating; }

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
}
