package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 160) String headline,
        @Size(max = 50000) String savedCvText,
        @Size(max = 5000) String skills,
        @Size(max = 5000) String preferences) {}
