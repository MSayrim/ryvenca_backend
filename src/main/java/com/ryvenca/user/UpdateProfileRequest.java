package com.ryvenca.user;

import java.util.List;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.WardrobeType;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 80, message = "İsim 1-80 karakter olmalı") String displayName,
        WardrobeType wardrobeType,
        @Size(max = 7, message = "En fazla 7 stil seçebilirsin") List<StylePreference> stylePreferences,
        Boolean onboardingCompleted) {
}
