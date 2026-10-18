package com.aicareeragent.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aicareeragent.dto.AnalysisRequest;
import com.aicareeragent.model.AnalysisRecord;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.AnalysisRecordRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.service.AiServiceClient;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

class AnalysisWorkflowTest {
    @Test void completedReportIsPersistedForTheOwnerAndSelectedOffer() {
        AiServiceClient ai = mock(AiServiceClient.class);
        UserRepository users = mock(UserRepository.class);
        AnalysisRecordRepository analyses = mock(AnalysisRecordRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserAccount owner = new UserAccount("Candidate", "candidate@example.test", "hash", Role.USER);
        JobOffer offer = new JobOffer("Backend engineer", "CloudCo", "Remote", "CDI", "Java and Spring");
        AnalysisRequest request = new AnalysisRequest("Java CV", "Java role", 4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("candidate@example.test");
        doReturn(Map.of("result", Map.of("compatibility_score", .87))).when(ai).analyze(request);
        when(users.findByEmailIgnoreCase("candidate@example.test")).thenReturn(Optional.of(owner));
        when(offers.findById(4L)).thenReturn(Optional.of(offer));
        when(analyses.save(any(AnalysisRecord.class))).thenAnswer(invocation -> {
            AnalysisRecord saved = invocation.getArgument(0);
            java.lang.reflect.Field field = AnalysisRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(saved, 12L);
            return saved;
        });
        AnalysisController controller = new AnalysisController(ai, users, analyses, offers);

        var response = controller.analyze(request, auth);

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<com.aicareeragent.model.AnalysisRecord> saved = ArgumentCaptor.forClass(com.aicareeragent.model.AnalysisRecord.class);
        verify(analyses).save(saved.capture());
        assertEquals(.87, saved.getValue().getCompatibilityScore());
        assertSame(offer, saved.getValue().getJobOffer());
    }

    @Test void incompleteAiResponseIsReturnedButNotCountedAsACompletedAnalysis() {
        AiServiceClient ai = mock(AiServiceClient.class);
        AnalysisRecordRepository analyses = mock(AnalysisRecordRepository.class);
        AnalysisRequest request = new AnalysisRequest("CV", "Role", null);
        doReturn(Map.of("result", Map.of("status", "FAILED"))).when(ai).analyze(request);
        AnalysisController controller = new AnalysisController(ai, mock(UserRepository.class), analyses, mock(JobOfferRepository.class));

        var response = controller.analyze(request, mock(Authentication.class));

        assertEquals(200, response.getStatusCode().value());
        verifyNoInteractions(analyses);
    }
}
