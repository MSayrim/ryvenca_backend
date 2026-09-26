package com.ryvenca.palette;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** A curated color combination as stored in the database (editable in the admin panel). */
@Entity
@Table(name = "palettes")
public class Palette {

    @Id
    private String id;

    /** Comma separated hex colors; the first one is the signature color. */
    @Column(nullable = false)
    private String colors;

    /** JSON object of admin-entered names per language code. */
    @Column(nullable = false)
    private String names = "{}";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Palette() {
    }

    public Palette(String id, List<String> colors, boolean builtIn, int sortOrder) {
        this.id = id;
        setColors(colors);
        this.builtIn = builtIn;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public List<String> getColors() {
        return Arrays.stream(colors.split(",")).map(String::trim).filter(c -> !c.isEmpty()).toList();
    }

    public void setColors(List<String> colors) {
        this.colors = String.join(",", colors);
    }

    public String getNamesJson() {
        return names;
    }

    public void setNamesJson(String names) {
        this.names = names;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
