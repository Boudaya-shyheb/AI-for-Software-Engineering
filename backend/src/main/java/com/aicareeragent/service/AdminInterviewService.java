package com.aicareeragent.service;

import com.aicareeragent.dto.AdminInterviewResult;
import com.aicareeragent.dto.AdminInterviewAnswerResult;
import com.aicareeragent.model.InterviewSession;
import com.aicareeragent.repository.InterviewSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminInterviewService {
    private static final TypeReference<List<Map<String, Object>>> EVALUATIONS_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> QUESTIONS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> SUMMARY_TYPE = new TypeReference<>() {};

    private final InterviewSessionRepository sessions;
    private final ObjectMapper mapper;

    public AdminInterviewService(InterviewSessionRepository sessions, ObjectMapper mapper) {
        this.sessions = sessions;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AdminInterviewResult> listAll() {
        return sessions.findAllByOrderByCreatedAtDesc().stream().map(this::toResult).toList();
    }

    private AdminInterviewResult toResult(InterviewSession session) {
        List<Map<String, Object>> questions = readList(session.getQuestionsJson(), QUESTIONS_TYPE);
        List<Map<String, Object>> evaluations = readList(session.getEvaluationsJson(), EVALUATIONS_TYPE);
        boolean completed = session.getSummaryJson() != null;
        Map<String, Object> summary = completed ? readSummary(session.getSummaryJson()) : Map.of();
        List<String> strengths = completed ? strings(summary.get("strengths")) : evaluationItems(evaluations, "strengths");
        List<String> weaknesses = completed ? strings(summary.get("weaknesses")) : evaluationItems(evaluations, "improvements");
        List<String> recommendations = completed ? strings(summary.get("recommendations")) : weaknesses.stream().limit(3).toList();
        Integer score = null;
        if (completed) score = number(summary.get("score"));
        else if (!evaluations.isEmpty()) score = averageScore(evaluations);
        return new AdminInterviewResult(session.getId(), session.getUser().getFullName(), session.getUser().getEmail(),
                session.getOfferTitle(), session.getCompany(), completed ? "COMPLETED" : "IN_PROGRESS",
                score, evaluations.size(), questions.size(), session.getCreatedAt(),
                evaluations.stream().map(this::toAnswerResult).toList(),
                strengths, weaknesses, recommendations);
    }

    private AdminInterviewAnswerResult toAnswerResult(Map<String, Object> evaluation) {
        return new AdminInterviewAnswerResult(
                Objects.toString(evaluation.get("question"), ""), Objects.toString(evaluation.get("type"), ""),
                evaluation.get("score") instanceof Number answerScore ? answerScore.intValue() : null,
                Objects.toString(evaluation.get("feedback"), ""), strings(evaluation.get("strengths")),
                strings(evaluation.get("improvements")));
    }

    private List<Map<String, Object>> readList(String json, TypeReference<List<Map<String, Object>>> type) {
        try { return mapper.readValue(json, type); }
        catch (Exception error) { throw new IllegalStateException("Could not read interview results", error); }
    }

    private Map<String, Object> readSummary(String json) {
        try { return mapper.readValue(json, SUMMARY_TYPE); }
        catch (Exception error) { throw new IllegalStateException("Could not read interview summary", error); }
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> items)) return List.of();
        return items.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static List<String> evaluationItems(List<Map<String, Object>> evaluations, String key) {
        return evaluations.stream().flatMap(item -> strings(item.get(key)).stream()).distinct().limit(5).toList();
    }

    private static int averageScore(List<Map<String, Object>> evaluations) {
        return (int) Math.round(evaluations.stream().map(item -> item.get("score")).filter(Number.class::isInstance)
                .map(Number.class::cast).mapToInt(Number::intValue).average().orElse(0));
    }

    private static int number(Object value) { return value instanceof Number score ? score.intValue() : 0; }
}
