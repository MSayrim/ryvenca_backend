package com.ryvenca.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ryvenca")
public record RyvencaProperties(Security security, Cors cors, Storage storage, Firebase firebase, Auth auth, Admin admin) {

    public record Security(String jwtSecret, Duration tokenTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String localDir, String publicBaseUrl, Duration draftRetention) {
    }

    /** Server-side Firebase (Admin SDK) configuration. */
    public record Firebase(String credentialsFile, String projectId) {
    }

    /** {@code localEnabled}: legacy e-mail/password accounts stored by this server (dev / no Firebase). */
    public record Auth(boolean localEnabled) {
    }

    /** E-mails that become admins when they sign in. */
    public record Admin(List<String> emails) {
    }
}
