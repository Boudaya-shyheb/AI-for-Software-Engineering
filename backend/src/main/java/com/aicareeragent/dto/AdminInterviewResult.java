package com.aicareeragent.dto;

import java.time.Instant;
import java.util.List;

public record AdminInterviewResult(
        Long id,
        String candidateName,
        String candidateEmail,
        String offerTitle,
        String company,
        String status,
        Integer score,
        int answeredQuestions,
        int totalQuestions,
        Instant createdAt,
        List<AdminInterviewAnswerResult> answers,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations) {}
