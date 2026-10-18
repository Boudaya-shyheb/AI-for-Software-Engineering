package com.aicareeragent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "progress_goals")
public class ProgressGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProgressPlan plan;

    @Column(nullable = false, length = 120)
    private String skillName;

    @Column(nullable = false, length = 240)
    private String objective;

    @Column(nullable = false, length = 240)
    private String activity;

    @Column(nullable = false, length = 240)
    private String exercise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgressStatus status = ProgressStatus.TO_DO;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected ProgressGoal() {}

    public ProgressGoal(ProgressPlan plan, String skillName, String objective, String activity, String exercise, int sortOrder) {
        this.plan = plan;
        this.skillName = normalize(skillName, "Compétence cible");
        this.objective = normalize(objective, "Renforcer la compétence ciblée");
        this.activity = normalize(activity, "Consacrer du temps à une activité structurée");
        this.exercise = normalize(exercise, "Mettre en pratique la compétence dans un cas concret");
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public ProgressPlan getPlan() { return plan; }
    public String getSkillName() { return skillName; }
    public String getObjective() { return objective; }
    public String getActivity() { return activity; }
    public String getExercise() { return exercise; }
    public ProgressStatus getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateStatus(ProgressStatus nextStatus) {
        if (!ProgressStatus.canTransition(this.status, nextStatus)) {
            throw new IllegalArgumentException("Transition de statut invalide: " + this.status + " -> " + nextStatus);
        }
        this.status = nextStatus;
        this.updatedAt = Instant.now();
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
