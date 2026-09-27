package com.aicareeragent.dto;

import com.aicareeragent.model.UserAccount;

public record ProfileResponse(Long id, String fullName, String email, String headline,
        String savedCvText, String skills, String preferences) {
    public static ProfileResponse from(UserAccount user) {
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getHeadline(),
                user.getSavedCvText(), user.getSkills(), user.getPreferences());
    }
}
