package com.aicareeragent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analysis_history")
public class AnalysisRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id")
    private JobOffer jobOffer;

    @Column(nullable = false)
    private double compatibilityScore;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AnalysisRecord() {}

    public AnalysisRecord(UserAccount user, JobOffer jobOffer, double compatibilityScore) {
        this.user = user;
        this.jobOffer = jobOffer;
        this.compatibilityScore = compatibilityScore;
    }

    public Long getId() { return id; }
    public JobOffer getJobOffer() { return jobOffer; }
    public double getCompatibilityScore() { return compatibilityScore; }
    public Instant getCreatedAt() { return createdAt; }
}
