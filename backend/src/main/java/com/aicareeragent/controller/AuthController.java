package com.aicareeragent.controller;

import com.aicareeragent.dto.AuthResponse;
import com.aicareeragent.dto.LoginRequest;
import com.aicareeragent.dto.RegisterRequest;
import com.aicareeragent.dto.UserSummary;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserSummary me(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName()).map(UserSummary::from).orElseThrow();
    }
}
