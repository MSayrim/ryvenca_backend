package com.ryvenca.outfit.engine;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Labeled;

/** Slot of a garment inside an outfit, in display order. */
public enum OutfitRole implements Labeled {
    OUTERWEAR(1.2),
    TOP(1.0),
    DRESS(1.6),
    BOTTOM(1.0),
    SHOES(0.45),
    BAG(0.4),
    ACCESSORY(0.15);

    private final double visualArea;

    OutfitRole(double visualArea) {
        this.visualArea = visualArea;
    }

    @Override
    public String labelKey() {
        return "role." + name();
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
