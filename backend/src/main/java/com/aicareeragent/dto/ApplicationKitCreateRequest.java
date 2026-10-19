package com.aicareeragent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationKitCreateRequest(
        @NotNull Long jobOfferId,
        @Size(max = 50000, message = "CV must not exceed 50000 characters") String cvText) {}
