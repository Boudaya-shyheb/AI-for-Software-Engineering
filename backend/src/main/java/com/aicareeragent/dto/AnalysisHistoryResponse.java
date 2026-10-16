package com.aicareeragent.dto;

import com.aicareeragent.model.AnalysisRecord;
import java.time.Instant;

public record AnalysisHistoryResponse(Long id, double compatibilityScore, Long jobOfferId,
        String jobTitle, String company, Instant createdAt) {
    public static AnalysisHistoryResponse from(AnalysisRecord record) {
        var offer = record.getJobOffer();
        return new AnalysisHistoryResponse(record.getId(), record.getCompatibilityScore(),
                offer == null ? null : offer.getId(), offer == null ? "Free analysis" : offer.getTitle(),
                offer == null ? "CareerLens" : offer.getCompany(), record.getCreatedAt());
    }
}
