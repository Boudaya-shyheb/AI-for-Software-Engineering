package com.aicareeragent.dto;

import com.aicareeragent.model.JobOffer;
import java.time.Instant;

public record JobOfferResponse(
        Long id,
        String title,
        String company,
        String location,
        String employmentType,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
    public static JobOfferResponse from(JobOffer offer) {
        return new JobOfferResponse(offer.getId(), offer.getTitle(), offer.getCompany(), offer.getLocation(),
                offer.getEmploymentType(), offer.getDescription(), offer.isActive(), offer.getCreatedAt(), offer.getUpdatedAt());
    }
}
