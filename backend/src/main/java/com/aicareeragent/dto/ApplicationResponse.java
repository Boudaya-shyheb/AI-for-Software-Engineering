package com.aicareeragent.dto;

import com.aicareeragent.model.JobApplication;
import java.time.Instant;

public record ApplicationResponse(Long id, Long jobOfferId, String jobTitle, String company, String location,
        Long userId, String candidateName, String candidateEmail, String status, String coverLetter,
        Instant createdAt, Instant updatedAt) {
    public static ApplicationResponse from(JobApplication value) {
        return new ApplicationResponse(value.getId(), value.getJobOffer().getId(), value.getJobOffer().getTitle(),
                value.getJobOffer().getCompany(), value.getJobOffer().getLocation(), value.getUser().getId(),
                value.getUser().getFullName(), value.getUser().getEmail(), value.getStatus().name(),
                value.getCoverLetter(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
