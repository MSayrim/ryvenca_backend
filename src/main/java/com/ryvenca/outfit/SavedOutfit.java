package com.ryvenca.outfit;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A user-saved outfit, identified by its sorted garment id key ("3-12-40"). */
@Entity
@Table(name = "saved_outfits")
public class SavedOutfit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "outfit_key", nullable = false)
    private String outfitKey;

    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedOutfit() {
    }

    public SavedOutfit(Long ownerId, String outfitKey, String title) {
        this.ownerId = ownerId;
        this.outfitKey = outfitKey;
        this.title = title;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public List<Long> garmentIds() {
        return Arrays.stream(outfitKey.split("-")).map(Long::parseLong).toList();
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOutfitKey() {
        return outfitKey;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
