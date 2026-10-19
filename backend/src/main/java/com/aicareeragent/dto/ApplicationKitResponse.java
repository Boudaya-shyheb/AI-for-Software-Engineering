package com.aicareeragent.dto;

import com.aicareeragent.model.ApplicationKit;
import java.time.Instant;
import java.util.List;

public record ApplicationKitResponse(
        Long id,
        Long jobOfferId,
        String offerTitle,
        String company,
        String adaptedCv,
        String coverLetter,
        List<String> matchedSkills,
        List<String> missingSkills,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationKitResponse from(ApplicationKit kit) {
        return new ApplicationKitResponse(
                kit.getId(),
                kit.getJobOffer().getId(),
                kit.getJobOffer().getTitle(),
                kit.getJobOffer().getCompany(),
                kit.getAdaptedCv(),
                kit.getCoverLetter(),
                kit.getMatchedSkills(),
                kit.getMissingSkills(),
                kit.getCreatedAt(),
                kit.getUpdatedAt()
        );
    }
}
