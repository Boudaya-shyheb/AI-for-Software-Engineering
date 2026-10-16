package com.aicareeragent.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aicareeragent.dto.ProfileRequest;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ProfileWorkflowTest {
    @Test void savedCvCanBeLoadedFromTheAuthenticatedProfileForReuse() {
        UserRepository users = mock(UserRepository.class);
        UserAccount owner = new UserAccount("Sahar Demo", "sahar@example.test", "hash", Role.USER);
        owner.updateProfile("Sahar Demo", "Full-stack engineer", "Angular Java Spring Boot", "Angular, Java", "Remote");
        when(users.findByEmailIgnoreCase("sahar@example.test")).thenReturn(Optional.of(owner));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("sahar@example.test");
        ProfileController controller = new ProfileController(users);

        var result = controller.get(auth);

        assertEquals("Angular Java Spring Boot", result.savedCvText());
        assertEquals("sahar@example.test", result.email());
        verify(users, never()).findByEmailIgnoreCase("other@example.test");
    }

    @Test void profileChangesPersistAndAreReturnedForTheNextMatchingSession() {
        UserRepository users = mock(UserRepository.class);
        UserAccount owner = new UserAccount("Sahar", "sahar@example.test", "hash", Role.USER);
        when(users.findByEmailIgnoreCase("sahar@example.test")).thenReturn(Optional.of(owner));
        when(users.save(owner)).thenReturn(owner);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("sahar@example.test");
        ProfileController controller = new ProfileController(users);

        var updated = controller.update(new ProfileRequest("Sahar Demo", "Backend engineer", "Java CV", "Java, Spring", "Hybrid"), auth);
        var reloaded = controller.get(auth);

        assertEquals("Java CV", updated.savedCvText());
        assertEquals("Java CV", reloaded.savedCvText());
        assertEquals("Backend engineer", reloaded.headline());
        verify(users).save(owner);
    }
}
