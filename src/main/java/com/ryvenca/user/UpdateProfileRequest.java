package com.ryvenca.user;

import java.util.List;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.WardrobeType;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 80, message = "{validation.displayName.size}") String displayName,
        WardrobeType wardrobeType,
        @Size(max = 7, message = "{validation.styles.size}") List<StylePreference> stylePreferences,
        Boolean onboardingCompleted,
        String language) {
}
