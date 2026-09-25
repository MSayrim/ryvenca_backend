package com.ryvenca.outfit.engine;

import java.util.List;
import java.util.stream.Collectors;

/** A fully scored outfit. Items are in display order (see {@link OutfitRole}). */
public record OutfitEvaluation(List<WardrobeItem> items, int score, double raw, ColorAnalysis color,
                               CompatibilityAnalysis compatibility, SeasonAnalysis season,
                               OccasionAnalysis occasion, StyleAnalysis style) {

    public static final double COLOR_WEIGHT = 0.40;
    public static final double CATEGORY_WEIGHT = 0.25;
    public static final double SEASON_WEIGHT = 0.15;
    public static final double OCCASION_WEIGHT = 0.15;
    public static final double STYLE_WEIGHT = 0.05;

    public String key() {
        return keyOf(items.stream().map(WardrobeItem::id).toList());
    }

    public List<Long> ids() {
        return items.stream().map(WardrobeItem::id).toList();
    }

    public WardrobeItem item(OutfitRole role) {
        return ColorHarmony.find(items, role);
    }

    public static String keyOf(List<Long> ids) {
        return ids.stream().sorted().map(String::valueOf).collect(Collectors.joining("-"));
    }
}
