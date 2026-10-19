package com.aicareeragent.dto;

import com.aicareeragent.model.ProgressStatus;
import jakarta.validation.constraints.NotNull;

public record GoalStatusUpdateRequest(@NotNull ProgressStatus status) {
}
