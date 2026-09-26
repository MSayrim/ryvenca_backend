package com.ryvenca.firebase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ryvenca.config.RyvencaProperties;

/**
 * Firebase Admin SDK, initialized from the service account JSON file configured in
 * {@code ryvenca.firebase.credentials-file} (default {@code ./config/firebase-service-account.json}).
 * Without the file the server runs without Firebase sign-in (local accounts only).
 */
@Component
public class FirebaseAdminGateway implements FirebaseAuthGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminGateway.class);
    private static final String APP_NAME = "ryvenca";

    private final FirebaseAuth auth;
    private final String projectId;

    public FirebaseAdminGateway(RyvencaProperties properties) {
        RyvencaProperties.Firebase config = properties.firebase();
        String file = config == null ? null : config.credentialsFile();
        FirebaseAuth initialized = null;
        String resolvedProject = null;
        if (file != null && !file.isBlank() && Files.isRegularFile(Path.of(file))) {
            try (InputStream in = Files.newInputStream(Path.of(file))) {
                FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in));
                if (config.projectId() != null && !config.projectId().isBlank()) {
                    options.setProjectId(config.projectId());
                }
                FirebaseApp app = FirebaseApp.getApps().stream().filter(a -> a.getName().equals(APP_NAME)).findFirst()
                        .orElseGet(() -> FirebaseApp.initializeApp(options.build(), APP_NAME));
                initialized = FirebaseAuth.getInstance(app);
                resolvedProject = app.getOptions().getProjectId();
                log.info("Firebase Admin initialized for project {}", resolvedProject);
            } catch (IOException | RuntimeException e) {
                log.error("Firebase credentials at {} could not be loaded; Firebase sign-in is disabled", file, e);
            }
        } else {
            log.warn("No Firebase service account at '{}'; Firebase sign-in is disabled (local accounts only)", file);
        }
        this.auth = initialized;
        this.projectId = resolvedProject;
    }

    @Override
    public boolean available() {
        return auth != null;
    }

    @Override
    public Optional<String> projectId() {
        return Optional.ofNullable(projectId);
    }

    @Override
    public VerifiedToken verify(String idToken) {
        if (auth == null) {
            throw new IllegalStateException("Firebase is not configured");
        }
        try {
            FirebaseToken token = auth.verifyIdToken(idToken, true);
            Object firebase = token.getClaims().get("firebase");
            String provider = firebase instanceof Map<?, ?> map && map.get("sign_in_provider") instanceof String p ? p : null;
            return new VerifiedToken(token.getUid(), token.getEmail(), token.isEmailVerified(), token.getName(), provider);
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid Firebase ID token", e);
        }
    }

    @Override
    public void deleteUser(String uid) {
        if (auth == null || uid == null) {
            return;
        }
        try {
            auth.deleteUser(uid);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                throw new IllegalStateException("Firebase user could not be deleted", e);
            }
        }
    }
}
