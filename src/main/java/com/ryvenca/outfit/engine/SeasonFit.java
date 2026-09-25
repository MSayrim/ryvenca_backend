package com.ryvenca.outfit.engine;

import static com.ryvenca.catalog.Subcategory.SANDAL;
import static com.ryvenca.catalog.Subcategory.SHORTS;
import static com.ryvenca.catalog.Subcategory.TANK_TOP;

import java.util.List;

import com.ryvenca.catalog.Season;
import com.ryvenca.outfit.engine.SeasonAnalysis.Warmth;

/** Season compatibility: do the pieces belong to the season, and is the outfit warm/light enough. */
final class SeasonFit {

    private SeasonFit() {
    }

    static SeasonAnalysis analyze(List<WardrobeItem> items, Season season) {
        double fits = 0;
        double weights = 0;
        for (WardrobeItem item : items) {
            double w = switch (item.role()) {
                case BAG -> 0.4;
                case ACCESSORY -> 0.3;
                default -> 1.0;
            };
            fits += item.fits(season) ? w : 0;
            weights += w;
        }
        double score = fits / weights;

        WardrobeItem outer = ColorHarmony.find(items, OutfitRole.OUTERWEAR);
        WardrobeItem top = ColorHarmony.find(items, OutfitRole.TOP);
        if (top == null) {
            top = ColorHarmony.find(items, OutfitRole.DRESS);
        }
        int layerWarmth = outer == null ? 0 : outer.subcategory().warmth();
        int topWarmth = top == null ? 0 : top.subcategory().warmth();
        boolean summerPieces = items.stream().anyMatch(i -> i.subcategory() == SHORTS || i.subcategory() == SANDAL
                || i.subcategory() == TANK_TOP);

        Warmth warmth = Warmth.OK;
        switch (season) {
            case WINTER -> {
                if (Math.max(layerWarmth, topWarmth) < 2) {
                    score -= 0.2;
                    warmth = Warmth.TOO_LIGHT;
                }
                if (summerPieces) {
                    score -= 0.3;
                    warmth = Warmth.TOO_LIGHT;
                }
            }
            case SUMMER -> {
                if (layerWarmth >= 2 || topWarmth >= 2) {
                    score -= 0.35;
                    warmth = Warmth.TOO_WARM;
                } else if (outer != null) {
                    score -= 0.08;
                }
            }
            case AUTUMN -> {
                if (summerPieces) {
                    score -= 0.2;
                    warmth = Warmth.TOO_LIGHT;
                }
                if (outer != null) {
                    score += 0.05;
                }
            }
            case SPRING -> {
                if (layerWarmth >= 3) {
                    score -= 0.12;
                    warmth = Warmth.TOO_WARM;
                }
            }
        }
        WardrobeItem layer = outer != null && season != Season.SUMMER && warmth == Warmth.OK ? outer : null;
        return new SeasonAnalysis(Math.max(0, Math.min(1, score)), season, warmth, layer);
    }
}
