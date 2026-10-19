package com.aicareeragent.dto;

import com.aicareeragent.model.ProgressPlan;
import java.time.Instant;
import java.util.List;

public record ProgressPlanResponse(
        Long id,
        Long analysisId,
        Long jobOfferId,
        String title,
        String summary,
        double progressPercentage,
        int totalGoals,
        int completedGoals,
        List<ProgressGoalResponse> goals,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProgressPlanResponse from(ProgressPlan plan) {
        return new ProgressPlanResponse(
                plan.getId(),
                plan.getAnalysis() == null ? null : plan.getAnalysis().getId(),
                plan.getJobOffer() == null ? null : plan.getJobOffer().getId(),
                plan.getTitle(),
                plan.getSummary(),
                plan.getProgressPercentage(),
                plan.getGoals().size(),
                plan.getCompletedGoalsCount(),
                plan.getGoals().stream().map(ProgressGoalResponse::from).toList(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
