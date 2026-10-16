package com.aicareeragent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CodeDebugRequest(
        @NotBlank @Pattern(regexp = "matching|offers|interviews|profile") String module,
        @NotBlank @Pattern(regexp = "python|java|javascript|typescript") String language,
        @NotBlank @Size(max = 20000) String code,
        @Size(max = 2000) String expected_behavior) {}
