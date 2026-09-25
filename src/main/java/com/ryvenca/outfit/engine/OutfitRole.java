package com.ryvenca.outfit.engine;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Labeled;

/** Slot of a garment inside an outfit, in display order. */
public enum OutfitRole implements Labeled {
    OUTERWEAR("Dış Giyim", 1.2),
    TOP("Üst", 1.0),
    DRESS("Elbise", 1.6),
    BOTTOM("Alt", 1.0),
    SHOES("Ayakkabı", 0.45),
    BAG("Çanta", 0.4),
    ACCESSORY("Aksesuar", 0.15);

    private final String label;
    private final double visualArea;

    OutfitRole(String label, double visualArea) {
        this.label = label;
        this.visualArea = visualArea;
    }

    @Override
    public String label() {
        return label;
    }

    /** Relative share of the silhouette the piece occupies; drives color weighting. */
    public double visualArea() {
        return visualArea;
    }

    /** Core pieces define the outfit; bags and accessories complete it. */
    public boolean isCore() {
        return this != BAG && this != ACCESSORY;
    }

    public static OutfitRole of(Category category) {
        return switch (category) {
            case TOP -> TOP;
            case BOTTOM -> BOTTOM;
            case OUTERWEAR -> OUTERWEAR;
            case DRESS -> DRESS;
            case SHOES -> SHOES;
            case BAG -> BAG;
            case ACCESSORY -> ACCESSORY;
        };
    }
}
