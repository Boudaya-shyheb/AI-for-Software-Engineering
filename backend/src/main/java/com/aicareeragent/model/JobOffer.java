package com.aicareeragent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_offers")
public class JobOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 140)
    private String company;

    @Column(nullable = false, length = 140)
    private String location;

    @Column(nullable = false, length = 80)
    private String employmentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected JobOffer() {}

    public JobOffer(String title, String company, String location, String employmentType, String description) {
        update(title, company, location, employmentType, description);
    }

    public void update(String title, String company, String location, String employmentType, String description) {
        this.title = title.trim();
        this.company = company.trim();
        this.location = location.trim();
        this.employmentType = employmentType.trim();
        this.description = description.trim();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getEmploymentType() { return employmentType; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
