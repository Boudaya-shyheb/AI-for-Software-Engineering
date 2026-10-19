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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "application_kits")
public class ApplicationKit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String adaptedCv;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String coverLetter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String matchedSkills = "";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String missingSkills = "";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected ApplicationKit() {}

    public ApplicationKit(UserAccount user, JobOffer jobOffer, String adaptedCv, String coverLetter,
            List<String> matchedSkills, List<String> missingSkills) {
        this.user = user;
        this.jobOffer = jobOffer;
        this.adaptedCv = adaptedCv;
        this.coverLetter = coverLetter;
        this.matchedSkills = joinSkills(matchedSkills);
        this.missingSkills = joinSkills(missingSkills);
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public JobOffer getJobOffer() { return jobOffer; }
    public String getAdaptedCv() { return adaptedCv; }
    public String getCoverLetter() { return coverLetter; }
    public List<String> getMatchedSkills() { return splitSkills(matchedSkills); }
    public List<String> getMissingSkills() { return splitSkills(missingSkills); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
        this.updatedAt = Instant.now();
    }

    private static String joinSkills(List<String> skills) {
        if (skills == null) {
            return "";
        }
        return skills.stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private static List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isEmpty())
                .toList();
    }
}
