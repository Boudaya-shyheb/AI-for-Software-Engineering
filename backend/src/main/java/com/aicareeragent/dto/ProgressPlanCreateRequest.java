package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProgressPlanCreateRequest(
        Long analysisId,
        Long jobOfferId,
        String title,
        String summary,
        List<String> missingSkills
) {
    public List<String> normalizedSkills() {
        if (missingSkills == null || missingSkills.isEmpty()) {
            return List.of();
        }
        return missingSkills.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim())
                .distinct()
                .toList();
    }
}
