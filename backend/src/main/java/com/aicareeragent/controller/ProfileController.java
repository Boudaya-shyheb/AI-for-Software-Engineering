package com.aicareeragent.controller;

import com.aicareeragent.dto.ProfileRequest;
import com.aicareeragent.dto.ProfileResponse;
import com.aicareeragent.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserRepository users;
    public ProfileController(UserRepository users) { this.users = users; }

    @GetMapping
    public ProfileResponse get(Authentication auth) { return ProfileResponse.from(find(auth)); }

    @PutMapping
    public ProfileResponse update(@Valid @RequestBody ProfileRequest request, Authentication auth) {
        var user = find(auth);
        user.updateProfile(request.fullName(), request.headline(), request.savedCvText(), request.skills(), request.preferences());
        return ProfileResponse.from(users.save(user));
    }

    private com.aicareeragent.model.UserAccount find(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
