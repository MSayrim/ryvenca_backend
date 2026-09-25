package com.ryvenca.user;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.WardrobeType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "wardrobe_type")
    private WardrobeType wardrobeType;

    @Convert(converter = StyleSetConverter.class)
    @Column(name = "style_preferences", nullable = false)
    private Set<StylePreference> stylePreferences = EnumSet.noneOf(StylePreference.class);

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public WardrobeType getWardrobeType() {
        return wardrobeType;
    }

    public void setWardrobeType(WardrobeType wardrobeType) {
        this.wardrobeType = wardrobeType;
    }

    public Set<StylePreference> getStylePreferences() {
        return stylePreferences;
    }

    public void setStylePreferences(Set<StylePreference> stylePreferences) {
        this.stylePreferences = stylePreferences.isEmpty()
                ? EnumSet.noneOf(StylePreference.class)
                : EnumSet.copyOf(stylePreferences);
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
