package com.aicareeragent.service;

import com.aicareeragent.dto.AuthResponse;
import com.aicareeragent.dto.LoginRequest;
import com.aicareeragent.dto.RegisterRequest;
import com.aicareeragent.dto.UserSummary;
import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists with this email");
        }
        UserAccount user = users.save(new UserAccount(
                request.fullName().trim(), email, encoder.encode(request.password()), Role.USER));
        return new AuthResponse(jwtService.generate(user), UserSummary.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow();
        return new AuthResponse(jwtService.generate(user), UserSummary.from(user));
    }
}
