package com.ryvenca.user;

/** How the account signs in. */
public enum AuthProvider {
    APPLE, GOOGLE, PASSWORD, LOCAL;

    /** Maps Firebase's {@code firebase.sign_in_provider} claim. */
    public static AuthProvider fromFirebase(String signInProvider) {
        if (signInProvider == null) {
            return PASSWORD;
        }
        return switch (signInProvider) {
            case "apple.com" -> APPLE;
            case "google.com" -> GOOGLE;
            default -> PASSWORD;
        };
    }
}
