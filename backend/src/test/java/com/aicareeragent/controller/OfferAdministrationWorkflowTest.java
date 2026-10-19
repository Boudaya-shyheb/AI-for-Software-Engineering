package com.aicareeragent.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aicareeragent.model.JobOffer;
import com.aicareeragent.repository.ApplicationKitRepository;
import com.aicareeragent.repository.FavoriteOfferRepository;
import com.aicareeragent.repository.JobApplicationRepository;
import com.aicareeragent.repository.JobOfferRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class OfferAdministrationWorkflowTest {
    @Test void archivingAnOfferRemovesItFromPublicRecommendationsWithoutDeletingIt() {
        JobOfferRepository offers = mock(JobOfferRepository.class);
        JobOffer offer = new JobOffer("Platform engineer", "OpsCo", "Tunis", "CDI", "Kubernetes");
        when(offers.findById(4L)).thenReturn(Optional.of(offer));
        when(offers.save(offer)).thenReturn(offer);
        AdminJobOfferController controller = new AdminJobOfferController(
                offers, mock(JobApplicationRepository.class), mock(FavoriteOfferRepository.class),
                mock(ApplicationKitRepository.class));

        var result = controller.active(4L, java.util.Map.of("active", false));

        assertFalse(result.active());
        verify(offers).save(offer);
        verify(offers, never()).delete(any());
    }

    @Test void linkedOffersCannotBeDeletedAndMustBeArchivedInstead() {
        JobOfferRepository offers = mock(JobOfferRepository.class);
        JobApplicationRepository applications = mock(JobApplicationRepository.class);
        FavoriteOfferRepository favorites = mock(FavoriteOfferRepository.class);
        ApplicationKitRepository applicationKits = mock(ApplicationKitRepository.class);
        when(applications.existsByJobOfferId(8L)).thenReturn(true);
        AdminJobOfferController controller = new AdminJobOfferController(
                offers, applications, favorites, applicationKits);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller.delete(8L));

        assertEquals(409, error.getStatusCode().value());
        verify(offers, never()).delete(any());
    }

    @Test void linkedApplicationKitBlocksOfferDeletion() {
        JobOfferRepository offers = mock(JobOfferRepository.class);
        JobApplicationRepository applications = mock(JobApplicationRepository.class);
        FavoriteOfferRepository favorites = mock(FavoriteOfferRepository.class);
        ApplicationKitRepository applicationKits = mock(ApplicationKitRepository.class);
        when(applicationKits.existsByJobOfferId(9L)).thenReturn(true);
        AdminJobOfferController controller = new AdminJobOfferController(
                offers, applications, favorites, applicationKits);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller.delete(9L));

        assertEquals(409, error.getStatusCode().value());
        verify(offers, never()).delete(any());
    }
}
