package com.ryvenca.image;

import java.time.Instant;
import java.util.UUID;

import com.ryvenca.color.ColorName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** An uploaded garment photo (original, normalized display version and 4:5 thumbnail). */
@Entity
@Table(name = "images")
public class ImageAsset {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "original_file", nullable = false)
    private String originalFile;

    @Column(name = "display_file", nullable = false)
    private String displayFile;

    @Column(name = "thumb_file", nullable = false)
    private String thumbFile;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_color")
    private ColorName detectedColor;

    @Column(name = "detected_hex")
    private String detectedHex;

    @Column(name = "detection_confidence")
    private Double detectionConfidence;

    @Column(name = "pattern_likely", nullable = false)
    private boolean patternLikely;

    @Column(nullable = false)
    private boolean attached;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImageAsset() {
    }

    public ImageAsset(UUID id, Long ownerId, String originalFile, String displayFile, String thumbFile, int width,
                      int height) {
        this.id = id;
        this.ownerId = ownerId;
        this.originalFile = originalFile;
        this.displayFile = displayFile;
        this.thumbFile = thumbFile;
        this.width = width;
        this.height = height;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void setDetection(ColorName color, String hex, double confidence, boolean patternLikely) {
        this.detectedColor = color;
        this.detectedHex = hex;
        this.detectionConfidence = confidence;
        this.patternLikely = patternLikely;
    }

    public UUID getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOriginalFile() {
        return originalFile;
    }

    public String getDisplayFile() {
        return displayFile;
    }

    public String getThumbFile() {
        return thumbFile;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ColorName getDetectedColor() {
        return detectedColor;
    }

    public String getDetectedHex() {
        return detectedHex;
    }

    public Double getDetectionConfidence() {
        return detectionConfidence;
    }

    public boolean isPatternLikely() {
        return patternLikely;
    }

    public boolean isAttached() {
        return attached;
    }

    public void setAttached(boolean attached) {
        this.attached = attached;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
