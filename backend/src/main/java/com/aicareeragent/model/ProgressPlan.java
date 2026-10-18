package com.aicareeragent.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "progress_plans")
public class ProgressPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AnalysisRecord analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id")
    private JobOffer jobOffer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary = "";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProgressGoal> goals = new ArrayList<>();

    protected ProgressPlan() {}

    public ProgressPlan(UserAccount user, AnalysisRecord analysis, JobOffer jobOffer, String title, String summary) {
        this.user = user;
        this.analysis = analysis;
        this.jobOffer = jobOffer;
        this.title = normalize(title, "Plan de progression");
        this.summary = normalize(summary, "Suggestions automatiques fondées sur les lacunes détectées.");
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public AnalysisRecord getAnalysis() { return analysis; }
    public JobOffer getJobOffer() { return jobOffer; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ProgressGoal> getGoals() { return goals; }

    public void setGoals(List<ProgressGoal> goals) {
        this.goals.clear();
        if (goals == null) {
            return;
        }
        for (ProgressGoal goal : goals) {
            goal = goal == null ? null : goal;
            if (goal != null) {
                goal = ensurePlan(goal);
                this.goals.add(goal);
            }
        }
        touch();
    }

    public void addGoal(ProgressGoal goal) {
        if (goal == null) {
            return;
        }
        if (goal.getPlan() != this) {
            goal = ensurePlan(goal);
        }
        this.goals.add(goal);
        touch();
    }

    public int getCompletedGoalsCount() {
        return (int) goals.stream().filter(goal -> goal.getStatus() == ProgressStatus.DONE).count();
    }

    public double getProgressPercentage() {
        if (goals.isEmpty()) {
            return 0;
        }
        return ((double) getCompletedGoalsCount() / goals.size()) * 100d;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    private ProgressGoal ensurePlan(ProgressGoal goal) {
        if (goal.getPlan() == this) {
            return goal;
        }
        goal = new ProgressGoal(this, goal.getSkillName(), goal.getObjective(), goal.getActivity(), goal.getExercise(), goal.getSortOrder());
        goal.updateStatus(goal.getStatus());
        return goal;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
