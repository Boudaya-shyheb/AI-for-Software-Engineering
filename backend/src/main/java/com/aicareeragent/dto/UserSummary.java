package com.aicareeragent.dto;

import com.aicareeragent.model.UserAccount;
import java.time.Instant;

public record UserSummary(Long id, String fullName, String email, String role, Instant createdAt) {
    public static UserSummary from(UserAccount user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }
}
