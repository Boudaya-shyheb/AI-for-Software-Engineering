package com.aicareeragent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aicareeragent.model.InterviewSession;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.InterviewSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminInterviewServiceTest {
    @Test void listsAnUnansweredSimulationWithoutAScore() throws Exception {
        UserAccount user = new UserAccount("Alex Candidate", "alex@example.com", "hash", Role.USER);
        InterviewSession session = new InterviewSession(user, 2L, "Java Developer", "Acme", "cv", "job",
                "[{\"question\":\"Q1\"}]", "{}");
        InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
        when(sessions.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(session));

        var result = new AdminInterviewService(sessions, new ObjectMapper()).listAll().getFirst();

        assertEquals("IN_PROGRESS", result.status());
        assertNull(result.score());
        assertEquals(0, result.answeredQuestions());
    }

    @Test void listsEverySessionWithOwnerAndProgressWithoutReturningAnswers() throws Exception {
        UserAccount user = new UserAccount("Alex Candidate", "alex@example.com", "hash", Role.USER);
        InterviewSession session = new InterviewSession(user, 2L, "Java Developer", "Acme", "private CV", "private job text",
                "[{\"question\":\"Q1\"},{\"question\":\"Q2\"}]", "{}");
        setField(session, "id", 12L);
        setField(session, "evaluationsJson", "[{\"question\":\"How did you use Java?\",\"type\":\"technical\",\"answer\":\"private answer\",\"score\":76,\"feedback\":\"Clear feedback\",\"strengths\":[\"Clear example\"],\"improvements\":[\"Add detail\"]}]");
        InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
        when(sessions.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(session));

        var results = new AdminInterviewService(sessions, new ObjectMapper()).listAll();

        assertEquals(1, results.size());
        assertEquals("alex@example.com", results.getFirst().candidateEmail());
        assertEquals("IN_PROGRESS", results.getFirst().status());
        assertEquals(76, results.getFirst().score());
        assertEquals(1, results.getFirst().answeredQuestions());
        assertEquals(2, results.getFirst().totalQuestions());
        assertEquals("How did you use Java?", results.getFirst().answers().getFirst().question());
        assertEquals(76, results.getFirst().answers().getFirst().score());
        assertEquals(List.of("Clear example"), results.getFirst().strengths());
        assertFalse(results.getFirst().toString().contains("private answer"));
        assertFalse(results.getFirst().toString().contains("private CV"));
    }

    @Test void exposesTheCompletedSummaryAndFinalScore() throws Exception {
        UserAccount user = new UserAccount("Alex Candidate", "alex@example.com", "hash", Role.USER);
        InterviewSession session = new InterviewSession(user, 2L, "Java Developer", "Acme", "cv", "job",
                "[{\"question\":\"Q1\"}]", "{}");
        setField(session, "summaryJson", "{\"score\":88,\"strengths\":[\"Good example\"],\"weaknesses\":[\"Be concise\"],\"recommendations\":[\"Practice\"]}");
        InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
        when(sessions.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(session));

        var result = new AdminInterviewService(sessions, new ObjectMapper()).listAll().getFirst();

        assertEquals("COMPLETED", result.status());
        assertEquals(88, result.score());
        assertEquals(List.of("Good example"), result.strengths());
        assertEquals(List.of("Be concise"), result.weaknesses());
        assertEquals(List.of("Practice"), result.recommendations());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
