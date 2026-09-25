package com.ryvenca.outfit.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Combines the five compatibility dimensions into the 0–100 "Uyum Skoru":
 * color 40%, garment/category 25%, season 15%, occasion 15%, style 5%.
 */
public final class OutfitScorer {

    private final PaletteFit paletteFit;

    OutfitScorer(PaletteFit paletteFit) {
        this.paletteFit = paletteFit;
    }

    PaletteFit paletteFit() {
        return paletteFit;
    }

    public OutfitEvaluation evaluate(List<WardrobeItem> pieces, OutfitRequest request) {
        return evaluate(pieces, request, true);
    }

    /** Search-time evaluation: identical score, without the palette naming used only for explanations. */
    OutfitEvaluation evaluateFast(List<WardrobeItem> pieces, OutfitRequest request) {
        return evaluate(pieces, request, false);
    }

    /** Adds the explanation-only details to an evaluation produced by {@link #evaluateFast}. */
    OutfitEvaluation detailed(OutfitEvaluation e, OutfitRequest request) {
        return evaluate(e.items(), request, true);
    }

    private OutfitEvaluation evaluate(List<WardrobeItem> pieces, OutfitRequest request, boolean detailed) {
        List<WardrobeItem> items = new ArrayList<>(pieces);
        items.sort(Comparator.comparingInt((WardrobeItem i) -> i.role().ordinal()).thenComparingLong(WardrobeItem::id));
        ColorAnalysis color = ColorHarmony.analyze(items, paletteFit, detailed);
        CompatibilityAnalysis compatibility = GarmentCompatibility.analyze(items);
        SeasonAnalysis season = SeasonFit.analyze(items, request.season());
        OccasionAnalysis occasion = OccasionFit.analyze(items, request.occasion());
        StyleAnalysis style = StyleFit.analyze(items, request.styles(), color, compatibility);
        double raw = OutfitEvaluation.COLOR_WEIGHT * color.score()
                + OutfitEvaluation.CATEGORY_WEIGHT * compatibility.score()
                + OutfitEvaluation.SEASON_WEIGHT * season.score()
                + OutfitEvaluation.OCCASION_WEIGHT * occasion.score()
                + OutfitEvaluation.STYLE_WEIGHT * style.score();
        return new OutfitEvaluation(List.copyOf(items), toDisplayScore(raw), raw, color, compatibility, season,
                occasion, style);
    }

    /** Cheap pre-score for pruning top/bottom pairs (color + garment compatibility only). */
    double pairScore(List<WardrobeItem> pieces) {
        return 0.6 * ColorHarmony.analyze(pieces, paletteFit, false).score() + 0.4 * GarmentCompatibility.analyze(pieces).score();
    }

    /**
     * The weighted mean is compressed near the top (a clashing outfit still scores ~0.75), so it is
     * stretched with a power curve: 0.97 → 95, 0.90 → 83, 0.75 → 60.
     */
    static int toDisplayScore(double raw) {
        return (int) Math.round(Math.pow(Math.max(0, Math.min(1, raw)), 1.8) * 100);
    }

    /** Validates the structure of an outfit; returns a Turkish reason or null when valid. */
    public static String structureProblem(List<WardrobeItem> items) {
        long tops = count(items, OutfitRole.TOP);
        long bottoms = count(items, OutfitRole.BOTTOM);
        long dresses = count(items, OutfitRole.DRESS);
        long shoes = count(items, OutfitRole.SHOES);
        if (shoes != 1) {
            return "Bir kombinde tam olarak bir ayakkabı olmalı.";
        }
        if (dresses > 1 || tops > 1 || bottoms > 1) {
            return "Bir kombinde her kategoriden en fazla bir parça olabilir.";
        }
        if (dresses == 1 && (tops > 0 || bottoms > 0)) {
            return "Elbise ile ayrıca üst ve alt seçilemez.";
        }
        if (dresses == 0 && (tops != 1 || bottoms != 1)) {
            return "Bir kombin bir üst ve bir alt (ya da bir elbise) içermeli.";
        }
        if (count(items, OutfitRole.OUTERWEAR) > 1 || count(items, OutfitRole.BAG) > 1) {
            return "Bir kombinde en fazla bir dış giyim ve bir çanta olabilir.";
        }
        if (count(items, OutfitRole.ACCESSORY) > 2) {
            return "Bir kombinde en fazla iki aksesuar olabilir.";
        }
        return null;
    }

    private static long count(List<WardrobeItem> items, OutfitRole role) {
        return items.stream().filter(i -> i.role() == role).count();
    }
}
