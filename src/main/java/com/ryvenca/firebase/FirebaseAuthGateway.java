package com.ryvenca.firebase;

import java.util.Optional;

/** The parts of Firebase Authentication the server needs. */
public interface FirebaseAuthGateway {

    /** A verified Firebase ID token. */
    record VerifiedToken(String uid, String email, boolean emailVerified, String name, String signInProvider) {
    }

    /** Whether server-side Firebase credentials are configured. */
    boolean available();

    Optional<String> projectId();

    /**
     * Verifies an ID token (signature, audience, expiry, revocation).
     *
     * @throws InvalidTokenException when the token is not valid
     */
    VerifiedToken verify(String idToken);

    /** Deletes the Firebase Authentication user; missing users are ignored. */
    void deleteUser(String uid);

    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
