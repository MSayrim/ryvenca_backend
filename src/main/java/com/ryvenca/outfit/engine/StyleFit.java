package com.ryvenca.outfit.engine;

import static com.ryvenca.catalog.Subcategory.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.Subcategory;

/** Which style the outfit expresses and how well that matches the user's preferences. */
final class StyleFit {

    private static final Set<Subcategory> STREET_SIGNALS =
            EnumSet.of(HOODIE, SWEATSHIRT, LEATHER_JACKET, DENIM_JACKET, CROSSBODY, HAT, PUFFER, BACKPACK);
    private static final Set<Subcategory> SPORT_SIGNALS =
            EnumSet.of(RUNNING_SHOE, SWEATPANTS, TANK_TOP, HOODIE);

    private StyleFit() {
    }

    static StyleAnalysis analyze(List<WardrobeItem> items, Set<StylePreference> preferences, ColorAnalysis color,
                                 CompatibilityAnalysis compatibility) {
        List<WardrobeItem> core = items.stream().filter(i -> i.role().isCore()).toList();
        Set<Subcategory> subs = EnumSet.noneOf(Subcategory.class);
        items.forEach(i -> subs.add(i.subcategory()));
        double formality = compatibility.formality();

        Map<StylePreference, Double> affinity = new EnumMap<>(StylePreference.class);
        for (StylePreference style : StylePreference.values()) {
            long matching = core.stream().filter(i -> i.subcategory().styles().contains(style)).count();
            double value = matching / (double) core.size();
            affinity.put(style, value * formalityPrior(style, formality));
        }

        boolean patterned = items.stream().anyMatch(WardrobeItem::pattern);
        boolean sporty = subs.stream().anyMatch(SPORT_SIGNALS::contains);
        boolean minimalPalette = color.allNeutral() && color.colors().size() <= 3 && !patterned && !sporty
                && formality >= 2.4;
        affinity.compute(StylePreference.MINIMAL, (k, v) -> minimalPalette ? Math.max(v, 0.75) : v * 0.6);
        if (!compatibility.highLow().isEmpty()) {
            affinity.compute(StylePreference.SMART_CASUAL, (k, v) -> Math.max(v, 0.9));
        }
        if (subs.stream().noneMatch(STREET_SIGNALS::contains)) {
            affinity.compute(StylePreference.STREETWEAR, (k, v) -> v * 0.7);
        }
        if (!sporty) {
            affinity.compute(StylePreference.SPORT, (k, v) -> v * 0.5);
        }

        // Ties are resolved in enum order (CASUAL, SMART_CASUAL, MINIMAL, CLASSIC, …).
        StylePreference style = StylePreference.CASUAL;
        for (StylePreference candidate : StylePreference.values()) {
            if (affinity.get(candidate) > affinity.get(style) + 1e-9) {
                style = candidate;
            }
        }
        double score;
        if (preferences == null || preferences.isEmpty()) {
            score = 0.8;
        } else {
            score = preferences.stream().mapToDouble(p -> Math.min(1, affinity.get(p) / 0.7)).max().orElse(0.8);
        }
        return new StyleAnalysis(score, style, affinity);
    }

    /** Each style lives in a formality band; outside of it the affinity fades. */
    private static double formalityPrior(StylePreference style, double formality) {
        double min;
        double max;
        switch (style) {
            case SPORT -> {
                min = 1.0;
                max = 1.9;
            }
            case STREETWEAR -> {
                min = 1.4;
                max = 2.8;
            }
            case CASUAL -> {
                min = 1.8;
                max = 3.0;
            }
            case SMART_CASUAL -> {
                min = 2.7;
                max = 3.8;
            }
            case MINIMAL -> {
                min = 2.2;
                max = 4.6;
            }
            case CLASSIC -> {
                min = 3.4;
                max = 4.7;
            }
            case BUSINESS -> {
                min = 4.0;
                max = 5.0;
            }
            default -> {
                return 1;
            }
        }
        double distance = formality < min ? min - formality : formality > max ? formality - max : 0;
        return 1 - 0.35 * Math.min(1, distance / 1.2);
    }
}
