package com.aicareeragent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(@NotNull Long jobOfferId, @Size(max = 10000) String coverLetter) {}
