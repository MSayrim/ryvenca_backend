package com.ryvenca.deletion;

import java.time.Instant;

import com.ryvenca.user.AuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Anonymized record of a completed deletion (proof of compliance without keeping personal data). */
@Entity
@Table(name = "deletion_log")
public class DeletionLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_hash", nullable = false)
    private String emailHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeletionMethod method;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeletionLogEntry() {
    }

    public DeletionLogEntry(String emailHash, AuthProvider authProvider, DeletionMethod method, String reason) {
        this.emailHash = emailHash;
        this.authProvider = authProvider;
        this.method = method;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public DeletionMethod getMethod() {
        return method;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
