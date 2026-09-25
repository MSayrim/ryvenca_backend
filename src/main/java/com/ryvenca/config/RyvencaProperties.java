package com.ryvenca.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ryvenca")
public record RyvencaProperties(Security security, Cors cors, Storage storage) {

    public record Security(String jwtSecret, Duration tokenTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String localDir, String publicBaseUrl, Duration draftRetention) {
    }
}
