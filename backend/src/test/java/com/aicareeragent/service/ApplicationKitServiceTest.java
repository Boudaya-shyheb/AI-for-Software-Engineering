package com.aicareeragent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.aicareeragent.dto.ApplicationKitCreateRequest;
import com.aicareeragent.model.ApplicationKit;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.ApplicationKitRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

class ApplicationKitServiceTest {
    private static final String EMAIL = "candidate@example.test";

    @Test
    void newKitHasEqualCreatedAndUpdatedAt() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(16L);

        // Act
        ApplicationKit kit = new ApplicationKit(user, offer, "Adapted CV", "Letter",
                List.of("java"), List.of("docker"));

        // Assert
        assertEquals(kit.getCreatedAt(), kit.getUpdatedAt());
    }

    @Test
    void updatingCoverLetterChangesOnlyUpdatedAt() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(17L);
        ApplicationKit kit = new ApplicationKit(user, offer, "Adapted CV", "Old letter",
                List.of("java"), List.of("docker"));
        Instant createdAt = kit.getCreatedAt();
        Instant previousUpdatedAt = kit.getUpdatedAt();

        // Act
        kit.updateCoverLetter("New letter");

        // Assert
        assertEquals(createdAt, kit.getCreatedAt());
        assertTrue(!kit.getUpdatedAt().isBefore(previousUpdatedAt));
        assertEquals("New letter", kit.getCoverLetter());
    }

    @Test
    void generateUsesProfileCvAndSavesCompleteApplicationKit() throws Exception {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(7L);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(7L)).thenReturn(Optional.of(offer));
        Map<?, ?> generated = generated(true);
        doReturn(generated).when(ai).generateApplication("Profile CV", offer.getTitle(),
                offer.getCompany(), offer.getDescription(), user.getFullName());
        when(kits.save(any(ApplicationKit.class))).thenAnswer(invocation -> {
            ApplicationKit saved = invocation.getArgument(0);
            setId(saved, 21L);
            return saved;
        });
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        var result = service.generate(EMAIL, new ApplicationKitCreateRequest(7L, null));

        // Assert
        assertEquals(21L, result.id());
        assertEquals(7L, result.jobOfferId());
        assertEquals("Backend engineer", result.offerTitle());
        assertEquals("CloudCo", result.company());
        assertEquals("Adapted CV", result.adaptedCv());
        assertEquals("Cover letter", result.coverLetter());
        assertEquals(List.of("java"), result.matchedSkills());
        assertEquals(List.of("docker"), result.missingSkills());
        verify(ai).generateApplication("Profile CV", offer.getTitle(), offer.getCompany(),
                offer.getDescription(), user.getFullName());
        verify(kits).save(any(ApplicationKit.class));
    }

    @Test
    void generatePrefersProvidedCvOverProfileCv() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(8L);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(8L)).thenReturn(Optional.of(offer));
        doReturn(generated(true)).when(ai).generateApplication(eq("Pasted CV"), any(), any(), any(), any());
        when(kits.save(any(ApplicationKit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        service.generate(EMAIL, new ApplicationKitCreateRequest(8L, " Pasted CV "));

        // Assert
        verify(ai).generateApplication("Pasted CV", offer.getTitle(), offer.getCompany(),
                offer.getDescription(), user.getFullName());
        verify(ai, never()).generateApplication("Profile CV", offer.getTitle(), offer.getCompany(),
                offer.getDescription(), user.getFullName());
    }

    @Test
    void generateRejectsWhenBothRequestAndProfileCvAreEmpty() {
        // Arrange
        UserAccount user = user("");
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(7L)).thenReturn(Optional.of(offer(7L)));
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(7L, "")));

        // Assert
        assertEquals(400, error.getStatusCode().value());
        verify(ai, never()).generateApplication(any(), any(), any(), any(), any());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void generateRejectsAdministratorBeforeCallingAi() {
        // Arrange
        UserAccount admin = new UserAccount("Admin", EMAIL, "hash", Role.ADMIN);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(admin));
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(7L, "CV")));

        // Assert
        assertEquals(403, error.getStatusCode().value());
        verify(ai, never()).generateApplication(any(), any(), any(), any(), any());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void generateRejectsUnknownOffer() {
        // Arrange
        UserAccount user = user("Profile CV");
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(99L)).thenReturn(Optional.empty());
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(99L, null)));

        // Assert
        assertEquals(404, error.getStatusCode().value());
        verify(ai, never()).generateApplication(any(), any(), any(), any(), any());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void generateRejectsArchivedOffer() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer archivedOffer = offer(10L);
        archivedOffer.setActive(false);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(10L)).thenReturn(Optional.of(archivedOffer));
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(10L, null)));

        // Assert
        assertEquals(409, error.getStatusCode().value());
        verify(ai, never()).generateApplication(any(), any(), any(), any(), any());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void generateRejectsUnverifiedDocumentsWithoutSavingKit() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(11L);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(11L)).thenReturn(Optional.of(offer));
        doReturn(generated(false)).when(ai).generateApplication(any(), any(), any(), any(), any());
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(11L, null)));

        // Assert
        assertEquals(502, error.getStatusCode().value());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void generateRejectsIncompleteAiResponseWithoutSavingKit() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(12L);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(12L)).thenReturn(Optional.of(offer));
        doReturn(Map.of(
                "cover_letter", "Cover letter",
                "matched_skills", List.of("java"),
                "missing_skills", List.of("docker"),
                "verification", Map.of("verified", true, "unsupported_skills", List.of())))
                .when(ai).generateApplication(any(), any(), any(), any(), any());
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(12L, null)));

        // Assert
        assertEquals(502, error.getStatusCode().value());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void aiServiceUnavailableReturnsBadGateway() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(15L);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(offers.findById(15L)).thenReturn(Optional.of(offer));
        doThrow(new ResourceAccessException("connection refused")).when(ai)
                .generateApplication(any(), any(), any(), any(), any());
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.generate(EMAIL, new ApplicationKitCreateRequest(15L, null)));

        // Assert
        assertEquals(502, error.getStatusCode().value());
        assertEquals("The AI service is unavailable", error.getReason());
        verify(kits, never()).save(any(ApplicationKit.class));
    }

    @Test
    void getRejectsKitOwnedByAnotherUser() {
        // Arrange
        UserAccount user = user("Profile CV");
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(kits.findByIdAndOwnerEmail(30L, EMAIL)).thenReturn(Optional.empty());
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.get(EMAIL, 30L));

        // Assert
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void updateCoverLetterReplacesLetterAndUpdatesTimestamp() throws Exception {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(13L);
        ApplicationKit kit = new ApplicationKit(user, offer, "Adapted CV", "Old letter",
                List.of("java"), List.of("docker"));
        setId(kit, 31L);
        Instant previousUpdatedAt = Instant.parse("2020-01-01T00:00:00Z");
        setField(kit, "updatedAt", previousUpdatedAt);
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(kits.findByIdAndOwnerEmail(31L, EMAIL)).thenReturn(Optional.of(kit));
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        var result = service.updateCoverLetter(EMAIL, 31L, "New letter");

        // Assert
        assertEquals("New letter", result.coverLetter());
        assertEquals("New letter", kit.getCoverLetter());
        assertNotEquals(previousUpdatedAt, result.updatedAt());
    }

    @Test
    void deleteRemovesApplicationKitOwnedByUser() {
        // Arrange
        UserAccount user = user("Profile CV");
        JobOffer offer = offer(14L);
        ApplicationKit kit = new ApplicationKit(user, offer, "Adapted CV", "Letter",
                List.of("java"), List.of());
        ApplicationKitRepository kits = mock(ApplicationKitRepository.class);
        JobOfferRepository offers = mock(JobOfferRepository.class);
        UserRepository users = mock(UserRepository.class);
        AiServiceClient ai = mock(AiServiceClient.class);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(kits.findByIdAndOwnerEmail(32L, EMAIL)).thenReturn(Optional.of(kit));
        ApplicationKitService service = new ApplicationKitService(kits, offers, users, ai);

        // Act
        service.delete(EMAIL, 32L);

        // Assert
        verify(kits).delete(kit);
    }

    private static UserAccount user(String savedCvText) {
        UserAccount user = new UserAccount("Candidate", EMAIL, "hash", Role.USER);
        user.updateProfile("Candidate", "", savedCvText, "", "");
        return user;
    }

    private static JobOffer offer(Long id) throws RuntimeException {
        JobOffer offer = new JobOffer("Backend engineer", "CloudCo", "Remote", "CDI",
                "Java Spring Boot Docker");
        try {
            setId(offer, id);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return offer;
    }

    private static Map<String, Object> generated(boolean verified) {
        return Map.of(
                "adapted_cv", "Adapted CV",
                "cover_letter", "Cover letter",
                "matched_skills", List.of("java"),
                "missing_skills", List.of("docker"),
                "verification", Map.of(
                        "verified", verified,
                        "unsupported_skills", List.of()));
    }

    private static void setId(Object target, Long id) throws Exception {
        setField(target, "id", id);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
