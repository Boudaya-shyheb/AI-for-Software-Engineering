package com.aicareeragent.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aicareeragent.dto.JobMatchRequest;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.service.AiServiceClient;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class JobOfferWorkflowTest {
    @Test void publicCatalogContainsOnlyActiveOffers() {
        JobOfferRepository offers = mock(JobOfferRepository.class);
        JobOffer active = new JobOffer("Frontend engineer", "WebCo", "Tunis", "CDI", "Angular role");
        when(offers.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(active));
        JobOfferController controller = new JobOfferController(offers, mock(AiServiceClient.class));

        var result = controller.list();

        assertEquals(1, result.size());
        assertEquals("Frontend engineer", result.getFirst().title());
        assertTrue(result.getFirst().active());
        verify(offers).findByActiveTrueOrderByCreatedAtDesc();
    }

    @Test void matchingDelegatesOnlyThePublishedCatalogAndReturnsRankedResults() throws Exception {
        JobOfferRepository offers = mock(JobOfferRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        JobOffer published = new JobOffer("Backend engineer", "CloudCo", "Remote", "CDI", "Java and Spring");
        Field id = JobOffer.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(published, 4L);
        when(offers.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(published));
        Map<String, Object> ranked = Map.of("matches", List.of(Map.of("offer", "Backend engineer", "compatibility_score", .91)), "total_offers", 1);
        doReturn(ranked).when(ai).matchJobs(eq("Java engineer"), anyList());
        JobOfferController controller = new JobOfferController(offers, ai);

        var result = controller.matches(new JobMatchRequest("Java engineer"));

        assertSame(ranked, result);
        verify(ai).matchJobs(eq("Java engineer"), argThat(catalog -> catalog.size() == 1
                && catalog.getFirst().get("title").equals("Backend engineer")
                && catalog.getFirst().get("description").equals("Java and Spring")));
    }
}
