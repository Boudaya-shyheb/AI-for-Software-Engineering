package com.aicareeragent.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(nullable = false) private Long offerId;
    @Column(nullable = false, length = 160) private String offerTitle;
    @Column(nullable = false, length = 140) private String company;
    @Column(nullable = false, columnDefinition = "TEXT") private String cvText;
    @Column(nullable = false, columnDefinition = "TEXT") private String jobDescription;
    @Column(nullable = false, columnDefinition = "TEXT") private String questionsJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String profileJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String evaluationsJson = "[]";
    @Column(columnDefinition = "TEXT") private String summaryJson;
    @Column(nullable = false) private int currentQuestion = 0;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    protected InterviewSession() {}
    public InterviewSession(UserAccount user, Long offerId, String offerTitle, String company, String cvText, String jobDescription, String questionsJson, String profileJson) {
        this.user = user; this.offerId = offerId; this.offerTitle = offerTitle; this.company = company;
        this.cvText = cvText; this.jobDescription = jobDescription; this.questionsJson = questionsJson; this.profileJson = profileJson;
    }
    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public Long getOfferId() { return offerId; }
    public String getOfferTitle() { return offerTitle; }
    public String getCompany() { return company; }
    public String getCvText() { return cvText; }
    public String getJobDescription() { return jobDescription; }
    public String getQuestionsJson() { return questionsJson; }
    public String getProfileJson() { return profileJson; }
    public String getEvaluationsJson() { return evaluationsJson; }
    public String getSummaryJson() { return summaryJson; }
    public int getCurrentQuestion() { return currentQuestion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void recordAnswer(String evaluationsJson, int currentQuestion, String summaryJson) {
        this.evaluationsJson = evaluationsJson; this.currentQuestion = currentQuestion;
        this.summaryJson = summaryJson; this.updatedAt = Instant.now();
    }
}
