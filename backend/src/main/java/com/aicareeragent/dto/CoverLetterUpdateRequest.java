package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoverLetterUpdateRequest(
        @NotBlank(message = "The cover letter cannot be empty")
        @Size(max = 10000, message = "The cover letter must not exceed 10000 characters")
        String coverLetter) {}
