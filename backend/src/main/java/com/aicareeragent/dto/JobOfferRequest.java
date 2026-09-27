package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobOfferRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 140) String company,
        @NotBlank @Size(max = 140) String location,
        @NotBlank @Size(max = 80) String employmentType,
        @NotBlank @Size(max = 20000) String description) {}
