package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(@NotBlank String cv_text, @NotBlank String job_description) {}
