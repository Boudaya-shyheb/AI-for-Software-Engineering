package com.aicareeragent.dto;

import com.aicareeragent.model.ProgressGoal;
import com.aicareeragent.model.ProgressStatus;
import java.time.Instant;

public record ProgressGoalResponse(
        Long id,
        String skillName,
        String objective,
        String activity,
        String exercise,
        ProgressStatus status,
        String statusLabel,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProgressGoalResponse from(ProgressGoal goal) {
        return new ProgressGoalResponse(
                goal.getId(),
                goal.getSkillName(),
                goal.getObjective(),
                goal.getActivity(),
                goal.getExercise(),
                goal.getStatus(),
                goal.getStatus().getLabel(),
                goal.getSortOrder(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
