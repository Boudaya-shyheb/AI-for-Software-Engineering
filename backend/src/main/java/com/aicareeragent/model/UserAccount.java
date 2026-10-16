package com.aicareeragent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 160)
    private String headline = "";

    @Column(columnDefinition = "TEXT")
    private String savedCvText = "";

    @Column(columnDefinition = "TEXT")
    private String skills = "";

    @Column(columnDefinition = "TEXT")
    private String preferences = "";

    protected UserAccount() {}

    public UserAccount(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public String getHeadline() { return headline == null ? "" : headline; }
    public String getSavedCvText() { return savedCvText == null ? "" : savedCvText; }
    public String getSkills() { return skills == null ? "" : skills; }
    public String getPreferences() { return preferences == null ? "" : preferences; }

    public void updateProfile(String fullName, String headline, String savedCvText, String skills, String preferences) {
        this.fullName = fullName.trim();
        this.headline = headline == null ? "" : headline.trim();
        this.savedCvText = savedCvText == null ? "" : savedCvText.trim();
        this.skills = skills == null ? "" : skills.trim();
        this.preferences = preferences == null ? "" : preferences.trim();
    }
}
