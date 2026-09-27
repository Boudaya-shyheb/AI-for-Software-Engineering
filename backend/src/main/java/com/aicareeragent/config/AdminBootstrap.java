package com.aicareeragent.config;

import com.aicareeragent.model.Role;
import com.aicareeragent.model.UserAccount;
import com.aicareeragent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String name;
    private final String userEmail;
    private final String userPassword;
    private final String userName;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
            @Value("${admin.email}") String email,
            @Value("${admin.password}") String password,
            @Value("${admin.name}") String name,
            @Value("${default-user.email}") String userEmail,
            @Value("${default-user.password}") String userPassword,
            @Value("${default-user.name}") String userName) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
        this.name = name;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.userName = userName;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new UserAccount(name, email.toLowerCase(), encoder.encode(password), Role.ADMIN));
        }
        if (!users.existsByEmailIgnoreCase(userEmail)) {
            users.save(new UserAccount(userName, userEmail.toLowerCase(), encoder.encode(userPassword), Role.USER));
        }
    }
}
