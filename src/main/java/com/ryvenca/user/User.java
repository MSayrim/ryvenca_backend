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

    /** Only for local (non-Firebase) accounts. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean disabled;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "wardrobe_type")
    private WardrobeType wardrobeType;

    @Convert(converter = StyleSetConverter.class)
    @Column(name = "style_preferences", nullable = false)
    private Set<StylePreference> stylePreferences = EnumSet.noneOf(StylePreference.class);

    /** Preferred UI language code (see {@link com.ryvenca.i18n.Language}); null = not chosen yet. */
    private String language;

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

    /** An account created from a verified Firebase sign-in. */
    public static User fromFirebase(String email, String firebaseUid, AuthProvider provider, boolean emailVerified,
                                    String displayName) {
        User user = new User(email, null, displayName);
        user.firebaseUid = firebaseUid;
        user.authProvider = provider;
        user.emailVerified = emailVerified;
        return user;
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

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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
