package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobMatchRequest(@NotBlank @Size(max = 50000) String cvText) {}
