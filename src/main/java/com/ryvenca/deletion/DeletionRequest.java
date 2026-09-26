package com.ryvenca.deletion;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A deletion request from someone who cannot sign in; reviewed by an admin. */
@Entity
@Table(name = "deletion_requests")
public class DeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String email;

    private String message;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeletionRequestStatus status = DeletionRequestStatus.PENDING;

    private String note;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected DeletionRequest() {
    }

    public DeletionRequest(String reference, String email, String message, String language) {
        this.reference = reference;
        this.email = email;
        this.message = message;
        this.language = language;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void resolve(DeletionRequestStatus status, String note, long adminId) {
        this.status = status;
        this.note = note;
        this.resolvedBy = adminId;
        this.resolvedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public String getLanguage() {
        return language;
    }

    public DeletionRequestStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
