package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewCreateRequest(@NotNull Long offerId, @NotBlank @Size(max = 50000) String cvText) {}
