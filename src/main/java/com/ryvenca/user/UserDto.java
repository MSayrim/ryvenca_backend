package com.ryvenca.user;

import java.time.Instant;
import java.util.List;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.WardrobeType;

public record UserDto(Long id, String email, String displayName, WardrobeType wardrobeType,
                      List<StylePreference> stylePreferences, boolean onboardingCompleted, Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getWardrobeType(),
                List.copyOf(user.getStylePreferences()), user.isOnboardingCompleted(), user.getCreatedAt());
    }
}
