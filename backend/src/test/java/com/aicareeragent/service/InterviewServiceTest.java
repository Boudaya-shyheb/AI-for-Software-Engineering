package com.aicareeragent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aicareeragent.dto.InterviewAnswerRequest;
import com.aicareeragent.dto.InterviewCreateRequest;
import com.aicareeragent.model.InterviewSession;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.InterviewSessionRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class InterviewServiceTest {
    private InterviewSessionRepository sessions;
    private JobOfferRepository offers;
    private UserRepository users;
    private AiServiceClient ai;
    private InterviewService service;
    private UserAccount user;

    @BeforeEach void setUp() throws Exception {
        sessions = mock(InterviewSessionRepository.class); offers = mock(JobOfferRepository.class);
        users = mock(UserRepository.class); ai = mock(AiServiceClient.class);
        service = new InterviewService(sessions, offers, users, ai, new ObjectMapper());
        user = new UserAccount("Candidate", "candidate@example.com", "hash", Role.USER);
        setField(user, "id", 7L);
        when(users.findByEmailIgnoreCase("candidate@example.com")).thenReturn(Optional.of(user));
    }

    @Test void createsQuestionsForSelectedOfferAndPersistsSession() throws Exception {
        JobOffer offer = new JobOffer("Backend developer", "Acme", "Remote", "CDI", "Java Spring PostgreSQL");
        setField(offer, "id", 3L);
        when(offers.findById(3L)).thenReturn(Optional.of(offer));
        doReturn(Map.of(
                "questions", List.of(Map.of("type", "technical", "question", "How do you use Java?")),
                "profile", Map.of("matching_skills", List.of("java")))).when(ai).generateInterview("Java developer", offer.getDescription(), 8);
        when(sessions.save(any(InterviewSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.create("candidate@example.com", new InterviewCreateRequest(3L, "Java developer"));

        assertEquals("Backend developer", result.get("offerTitle"));
        assertEquals(1, ((List<?>) result.get("questions")).size());
        assertEquals(Map.of("matching_skills", List.of("java")), result.get("profile"));
        verify(sessions).save(any(InterviewSession.class));
    }

    @Test void evaluatesAndPersistsTheNextAnswerForTheOwner() throws Exception {
        InterviewSession session = new InterviewSession(user, 3L, "Backend developer", "Acme", "Java developer", "Java role",
                "[{\"type\":\"technical\",\"question\":\"How do you use Java?\"}]", "{}");
        setField(session, "id", 9L);
        when(sessions.findByIdAndUserId(9L, 7L)).thenReturn(Optional.of(session));
        doReturn(Map.of("score", 80, "feedback", "Clear", "strengths", List.of("Example"), "improvements", List.of()))
                .when(ai).evaluateInterviewAnswer(anyString(), eq("I shipped an API"), anyString(), anyString(), anyString());
        doReturn(Map.of("score", 80, "strengths", List.of("Example"), "weaknesses", List.of(), "recommendations", List.of("Keep it up")))
                .when(ai).summarizeInterview(anyList());

        Map<String, Object> result = service.answer("candidate@example.com", 9L, new InterviewAnswerRequest("I shipped an API"));

        assertEquals(true, result.get("completed"));
        assertEquals(1, result.get("currentQuestion"));
        assertEquals(80, ((Map<?, ?>) result.get("summary")).get("score"));
        assertThrows(ResponseStatusException.class, () -> service.answer("candidate@example.com", 9L, new InterviewAnswerRequest("A second answer")));
        verify(sessions, times(2)).findByIdAndUserId(9L, 7L);
    }

    @Test void refusesToReadAnotherUsersSession() {
        when(sessions.findByIdAndUserId(9L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.get("candidate@example.com", 9L));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
