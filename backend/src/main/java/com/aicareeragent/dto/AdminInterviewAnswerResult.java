package com.aicareeragent.dto;

import java.util.List;

public record AdminInterviewAnswerResult(
        String question,
        String type,
        Integer score,
        String feedback,
        List<String> strengths,
        List<String> improvements) {}
