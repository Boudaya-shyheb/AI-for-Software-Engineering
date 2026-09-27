package com.aicareeragent.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_applications", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_offer_id"}))
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.SENT;
    @Column(columnDefinition = "TEXT") private String coverLetter;
    @Column(nullable = false, columnDefinition = "TEXT") private String cvSnapshot;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    protected JobApplication() {}
    public JobApplication(UserAccount user, JobOffer offer, String coverLetter, String cvSnapshot) {
        this.user = user; this.jobOffer = offer; this.coverLetter = coverLetter == null ? "" : coverLetter.trim();
        this.cvSnapshot = cvSnapshot; }
    public void changeStatus(ApplicationStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public Long getId() { return id; } public UserAccount getUser() { return user; }
    public JobOffer getJobOffer() { return jobOffer; } public ApplicationStatus getStatus() { return status; }
    public String getCoverLetter() { return coverLetter; } public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
