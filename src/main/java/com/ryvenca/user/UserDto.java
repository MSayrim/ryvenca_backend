package com.ryvenca.user;

import java.time.Instant;
import java.util.List;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.WardrobeType;

public record UserDto(Long id, String email, String displayName, WardrobeType wardrobeType, String language,
                      List<StylePreference> stylePreferences, boolean onboardingCompleted, Role role,
                      AuthProvider authProvider, boolean emailVerified, Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getWardrobeType(), user.getLanguage(),
                List.copyOf(user.getStylePreferences()), user.isOnboardingCompleted(), user.getRole(),
                user.getAuthProvider(), user.isEmailVerified(), user.getCreatedAt());
    }
}
