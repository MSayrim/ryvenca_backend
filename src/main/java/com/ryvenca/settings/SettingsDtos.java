package com.ryvenca.settings;

/** Shapes of {@code GET /api/config} and the admin settings (see docs/API.md). */
public final class SettingsDtos {

    private SettingsDtos() {
    }

    public record Providers(Boolean apple, Boolean google, Boolean email) {
    }

    public record FirebaseWeb(String apiKey, String authDomain, String projectId, String appId, String messagingSenderId,
                              String storageBucket) {

        boolean complete() {
            return present(apiKey) && present(authDomain) && present(projectId) && present(appId);
        }

        private static boolean present(String s) {
            return s != null && !s.isBlank();
        }
    }

    public record Links(String privacyPolicy, String terms, String support, String supportEmail, String accountDeletion,
                        String appStore, String playStore) {
    }

    public record Maintenance(Boolean enabled, String message) {
    }

    public record MinVersion(String ios, String android) {
    }

    public record AuthConfig(boolean firebase, boolean local, Providers providers) {
    }

    public record PublicConfig(AuthConfig auth, FirebaseWeb firebaseWeb, Links links, Maintenance maintenance,
                               MinVersion minVersion) {
    }

    public record Status(boolean firebaseAdmin, String firebaseProjectId, boolean localAuth) {
    }

    public record AdminSettings(Providers providers, FirebaseWeb firebaseWeb, Links links, Maintenance maintenance,
                                MinVersion minVersion, Status status) {
    }

    /** Partial update: only non-null parts/fields are applied; an empty string clears a value. */
    public record SettingsUpdate(Providers providers, FirebaseWeb firebaseWeb, Links links, Maintenance maintenance,
                                 MinVersion minVersion) {
    }
}
