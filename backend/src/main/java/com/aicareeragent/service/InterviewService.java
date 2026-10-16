package com.aicareeragent.service;

import com.aicareeragent.dto.InterviewAnswerRequest;
import com.aicareeragent.dto.InterviewCreateRequest;
import com.aicareeragent.model.InterviewSession;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.InterviewSessionRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InterviewService {
    private final InterviewSessionRepository sessions;
    private final JobOfferRepository offers;
    private final UserRepository users;
    private final AiServiceClient ai;
    private final ObjectMapper mapper;
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};

    public InterviewService(InterviewSessionRepository sessions, JobOfferRepository offers, UserRepository users, AiServiceClient ai, ObjectMapper mapper) {
        this.sessions = sessions; this.offers = offers; this.users = users; this.ai = ai; this.mapper = mapper;
    }

    @Transactional
    public Map<String, Object> create(String email, InterviewCreateRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        JobOffer offer = offers.findById(request.offerId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job offer not found"));
        Map<?, ?> generated = ai.generateInterview(request.cvText(), offer.getDescription(), 8);
        Object questionValue = generated.get("questions");
        if (!(questionValue instanceof List<?> questions) || questions.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI service returned no interview questions");
        Object profile = generated.containsKey("profile") ? generated.get("profile") : Map.of();
        InterviewSession session = sessions.save(new InterviewSession(user, offer.getId(), offer.getTitle(), offer.getCompany(), request.cvText(), offer.getDescription(), write(questionValue), write(profile)));
        return view(session);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String email) {
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return sessions.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String email, Long id) { return view(owned(email, id)); }

    @Transactional
    public Map<String, Object> answer(String email, Long id, InterviewAnswerRequest request) {
        InterviewSession session = owned(email, id);
        List<Map<String, Object>> questions = read(session.getQuestionsJson());
        List<Map<String, Object>> evaluations = read(session.getEvaluationsJson());
        if (session.getSummaryJson() != null || session.getCurrentQuestion() >= questions.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "This interview is already complete");
        Map<String, Object> question = questions.get(session.getCurrentQuestion());
        Map<?, ?> evaluation = ai.evaluateInterviewAnswer(Objects.toString(question.get("question"), ""), request.answer(), session.getCvText(), session.getJobDescription(), Objects.toString(question.get("type"), "behavioral"));
        Map<String, Object> savedEvaluation = new LinkedHashMap<>();
        savedEvaluation.put("question", question.get("question")); savedEvaluation.put("type", question.get("type"));
        savedEvaluation.put("answer", request.answer()); savedEvaluation.putAll((Map<String, Object>) evaluation);
        evaluations.add(savedEvaluation);
        int next = session.getCurrentQuestion() + 1;
        String summary = next == questions.size() ? write(ai.summarizeInterview(evaluations)) : null;
        session.recordAnswer(write(evaluations), next, summary);
        Map<String, Object> response = view(session);
        response.put("evaluation", savedEvaluation);
        return response;
    }

    private InterviewSession owned(String email, Long id) {
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return sessions.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));
    }

    private Map<String, Object> view(InterviewSession s) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", s.getId()); result.put("offerId", s.getOfferId()); result.put("offerTitle", s.getOfferTitle());
        result.put("company", s.getCompany()); result.put("questions", read(s.getQuestionsJson()));
        result.put("profile", readObject(s.getProfileJson()));
        result.put("evaluations", read(s.getEvaluationsJson())); result.put("currentQuestion", s.getCurrentQuestion());
        result.put("completed", s.getSummaryJson() != null); result.put("summary", s.getSummaryJson() == null ? null : readObject(s.getSummaryJson()));
        result.put("createdAt", s.getCreatedAt()); result.put("updatedAt", s.getUpdatedAt()); return result;
    }
    private String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Could not store interview data", e); } }
    private List<Map<String, Object>> read(String value) { try { return mapper.readValue(value, LIST_OF_MAPS); } catch (Exception e) { throw new IllegalStateException("Could not read interview data", e); } }
    private Map<String, Object> readObject(String value) { try { return mapper.readValue(value, new TypeReference<>() {}); } catch (Exception e) { throw new IllegalStateException("Could not read interview summary", e); } }
}
