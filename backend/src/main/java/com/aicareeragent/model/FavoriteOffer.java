package com.aicareeragent.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "favorite_offers", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_offer_id"}))
public class FavoriteOffer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "job_offer_id", nullable = false) private JobOffer jobOffer;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected FavoriteOffer() {}
    public FavoriteOffer(UserAccount user, JobOffer offer) { this.user=user; this.jobOffer=offer; }
    public Long getId() { return id; } public JobOffer getJobOffer() { return jobOffer; }
}
