package com.ryvenca.garment;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.Subcategory;
import com.ryvenca.color.ColorName;
import com.ryvenca.image.ImageAsset;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "garments")
public class Garment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageAsset image;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Subcategory subcategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorName color;

    @Column(name = "color_hex", nullable = false)
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_source", nullable = false)
    private ColorSource colorSource;

    @Column(nullable = false)
    private boolean pattern;

    @Convert(converter = SeasonSetConverter.class)
    @Column(nullable = false)
    private Set<Season> seasons = EnumSet.noneOf(Season.class);

    @Convert(converter = OccasionSetConverter.class)
    @Column(nullable = false)
    private Set<Occasion> occasions = EnumSet.noneOf(Occasion.class);

    @Column(nullable = false)
    private boolean favorite;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Garment() {
    }

    public Garment(Long ownerId) {
        this.ownerId = ownerId;
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

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public ImageAsset getImage() {
        return image;
    }

    public void setImage(ImageAsset image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public Subcategory getSubcategory() {
        return subcategory;
    }

    public void setType(Subcategory subcategory) {
        this.subcategory = subcategory;
        this.category = subcategory.category();
    }

    public ColorName getColor() {
        return color;
    }

    public String getColorHex() {
        return colorHex;
    }

    public ColorSource getColorSource() {
        return colorSource;
    }

    public void setColor(ColorName color, String colorHex, ColorSource source) {
        this.color = color;
        this.colorHex = colorHex;
        this.colorSource = source;
    }

    public boolean isPattern() {
        return pattern;
    }

    public void setPattern(boolean pattern) {
        this.pattern = pattern;
    }

    public Set<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(Set<Season> seasons) {
        this.seasons = EnumSet.copyOf(seasons);
    }

    public Set<Occasion> getOccasions() {
        return occasions;
    }

    public void setOccasions(Set<Occasion> occasions) {
        this.occasions = EnumSet.copyOf(occasions);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
