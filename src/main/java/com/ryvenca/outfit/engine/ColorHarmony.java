package com.ryvenca.outfit.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ryvenca.color.ColorName;
import com.ryvenca.color.ColorScience;
import com.ryvenca.color.ColorTone;
import com.ryvenca.outfit.engine.ColorAnalysis.ColorCase;

/**
 * Color compatibility = 50% closeness to a curated palette (CIEDE2000, near colors accepted) +
 * 50% color theory rules (number of statement colors, hue relations, tonal dressing, near-clashes,
 * pattern mixing, repetition).
 */
final class ColorHarmony {

    private ColorHarmony() {
    }

    static ColorAnalysis analyze(List<WardrobeItem> items, PaletteFit fit) {
        // Distinct colors by visual area.
        Map<ColorName, Double> area = new LinkedHashMap<>();
        Map<ColorName, WardrobeItem> representative = new LinkedHashMap<>();
        for (WardrobeItem item : items) {
            double a = item.role().visualArea();
            area.merge(item.color(), a, Double::sum);
            WardrobeItem current = representative.get(item.color());
            if (current == null || current.role().visualArea() < a) {
                representative.put(item.color(), item);
            }
        }
        List<ColorName> colors = new ArrayList<>(area.keySet());
        colors.sort((x, y) -> Double.compare(area.get(y), area.get(x)));

        List<WardrobeItem> core = items.stream().filter(i -> i.role() != OutfitRole.ACCESSORY).toList();
        List<ColorName> neutrals = new ArrayList<>();
        List<ColorName> accents = new ArrayList<>();
        List<ColorName> softs = new ArrayList<>();
        Map<ColorName, Integer> occurrences = new LinkedHashMap<>();
        for (ColorName color : colors) {
            List<WardrobeItem> withColor = core.stream().filter(i -> i.color() == color).toList();
            if (withColor.isEmpty()) {
                continue;
            }
            occurrences.put(color, withColor.size());
            // A color is neutral if any of its pieces is worn as a neutral (e.g. denim blue).
            boolean neutral = withColor.stream().anyMatch(i -> i.tone() == ColorTone.NEUTRAL || i.tone() == ColorTone.EARTH);
            if (neutral) {
                neutrals.add(color);
            } else if (withColor.getFirst().tone() == ColorTone.SOFT) {
                softs.add(color);
            } else {
                accents.add(color);
            }
        }
        List<ColorName> statement = new ArrayList<>(accents);
        statement.addAll(softs);

        double palette = 0;
        int paletteIndex = -1;
        double bestEvidence = 0;
        for (int p = 0; p < fit.library().size(); p++) {
            palette = Math.max(palette, paletteScore(items, fit, p, core));
            double evidence = paletteEvidence(core, fit, p);
            if (evidence > bestEvidence) {
                bestEvidence = evidence;
                paletteIndex = p;
            }
        }
        boolean paletteMatched = paletteIndex >= 0;

        ColorCase colorCase;
        double rule;
        Set<ColorFamily> families = EnumSet.noneOf(ColorFamily.class);
        core.forEach(i -> families.add(ColorFamily.of(i.color())));
        double lightnessSpread = core.stream().mapToDouble(i -> i.lab().l()).max().orElse(0)
                - core.stream().mapToDouble(i -> i.lab().l()).min().orElse(0);
        long distinctCore = core.stream().map(WardrobeItem::color).distinct().count();

        if (distinctCore == 1) {
            colorCase = ColorCase.SINGLE_COLOR;
            rule = statement.isEmpty() ? 0.9 : 0.8;
        } else if (families.size() == 1 && families.contains(ColorFamily.ACHROMATIC)) {
            colorCase = ColorCase.MONOCHROME;
            rule = 0.9;
        } else if (families.size() == 1 && lightnessSpread >= 12) {
            colorCase = ColorCase.TONAL;
            rule = 0.94;
        } else if (accents.isEmpty() && softs.isEmpty()) {
            colorCase = ColorCase.ALL_NEUTRAL;
            rule = 0.88;
        } else if (accents.isEmpty() && softs.size() == 1) {
            colorCase = ColorCase.SINGLE_ACCENT;
            rule = 0.94;
        } else if (accents.isEmpty() && softs.size() == 2) {
            colorCase = ColorCase.SOFT_PAIR;
            rule = 0.86;
        } else if (accents.size() == 1 && softs.isEmpty()) {
            colorCase = ColorCase.SINGLE_ACCENT;
            rule = 0.95;
        } else if (statement.size() == 2) {
            double hue = ColorScience.hueDistance(hueOf(representative.get(statement.get(0))),
                    hueOf(representative.get(statement.get(1))));
            if (hue < 35) {
                colorCase = ColorCase.ANALOGOUS;
                rule = 0.86;
            } else if (hue >= 150) {
                colorCase = ColorCase.COMPLEMENTARY;
                rule = 0.8;
            } else {
                colorCase = ColorCase.DISCORDANT;
                rule = 0.55;
            }
            boolean bothLoud = statement.stream()
                    .allMatch(c -> representative.get(c).lab().chroma() > 45);
            if (bothLoud) {
                rule -= 0.08;
            }
        } else {
            colorCase = ColorCase.BUSY;
            rule = 0.35;
        }

        boolean nearClash = nearClash(items);
        if (nearClash) {
            rule -= 0.12;
        }
        long patterned = core.stream().filter(WardrobeItem::pattern).count();
        boolean patternClash = patterned >= 2;
        if (patternClash) {
            rule -= 0.22;
        } else if (patterned == 1 && statement.size() >= 2) {
            rule -= 0.08;
        }
        boolean bagShoeEcho = bagShoeEcho(items);
        if (bagShoeEcho) {
            rule += 0.03;
        }
        boolean accentEcho = statement.stream().anyMatch(c -> occurrences.getOrDefault(c, 0) >= 2);
        if (accentEcho) {
            rule += 0.03;
        }
        rule = clamp(rule);

        double score = clamp(0.5 * palette + 0.5 * rule);
        return new ColorAnalysis(score, paletteIndex, palette, paletteMatched, rule, colorCase, colors, neutrals, statement, nearClash,
                patternClash, bagShoeEcho, accentEcho);
    }

    private static double paletteScore(List<WardrobeItem> items, PaletteFit fit, int p, List<WardrobeItem> core) {
        double covered = 0;
        double weights = 0;
        Set<Integer> used = new java.util.HashSet<>();
        for (WardrobeItem item : items) {
            double w = item.role().visualArea();
            double f = fit.fit(item, p);
            ColorTone tone = item.tone();
            if (tone == ColorTone.NEUTRAL) {
                f = Math.max(f, item.lab().chroma() < 8 ? 0.8 : 0.6);
            }
            covered += w * f;
            weights += w;
            int swatch = fit.swatch(item, p);
            if (swatch >= 0 && item.role() != OutfitRole.ACCESSORY) {
                used.add(swatch);
            }
        }
        long distinct = core.stream().map(WardrobeItem::color).distinct().count();
        int paletteSize = fit.library().get(p).swatches().size();
        double wanted = Math.max(1, Math.min(Math.min(distinct, 3), paletteSize));
        double usage = Math.min(1, used.size() / wanted);
        return 0.8 * (covered / weights) + 0.2 * usage;
    }

    /**
     * How literally the outfit's own colors come from palette {@code p} (no neutral allowance), or 0
     * when the palette should not be named: every core piece (one may miss from five on) must be close
     * to a swatch, at least two swatches must be used and one of them must be the signature color.
     */
    private static double paletteEvidence(List<WardrobeItem> core, PaletteFit fit, int p) {
        long misses = core.stream().filter(i -> fit.fit(i, p) < 0.75).count();
        List<Integer> used = core.stream().map(i -> fit.swatch(i, p)).filter(s -> s >= 0).distinct().toList();
        // The first swatch is the palette's signature color ("Turuncu Enerji" needs the orange).
        if (misses > (core.size() >= 5 ? 1 : 0) || used.size() < 2 || !used.contains(0)) {
            return 0;
        }
        double covered = 0;
        double weights = 0;
        for (WardrobeItem item : core) {
            covered += item.role().visualArea() * fit.fit(item, p);
            weights += item.role().visualArea();
        }
        return covered / weights;
    }

    /** Pieces that are almost, but not quite, the same color look accidental (e.g. navy with black). */
    private static boolean nearClash(List<WardrobeItem> items) {
        WardrobeItem bottom = find(items, OutfitRole.BOTTOM);
        if (bottom == null) {
            return false;
        }
        for (OutfitRole role : List.of(OutfitRole.TOP, OutfitRole.OUTERWEAR)) {
            WardrobeItem upper = find(items, role);
            if (upper == null || (upper.isDenim() && bottom.isDenim())) {
                continue;
            }
            double de = ColorScience.deltaE2000(upper.lab(), bottom.lab());
            double dl = Math.abs(upper.lab().l() - bottom.lab().l());
            if (de > 3.5 && de < 10 && dl < 8) {
                return true;
            }
        }
        return false;
    }

    private static boolean bagShoeEcho(List<WardrobeItem> items) {
        WardrobeItem bag = find(items, OutfitRole.BAG);
        WardrobeItem shoes = find(items, OutfitRole.SHOES);
        if (bag == null || shoes == null) {
            return false;
        }
        return bag.color() == shoes.color() || ColorScience.deltaE2000(bag.lab(), shoes.lab()) < 12;
    }

    static WardrobeItem find(List<WardrobeItem> items, OutfitRole role) {
        for (WardrobeItem item : items) {
            if (item.role() == role) {
                return item;
            }
        }
        return null;
    }

    private static double hueOf(WardrobeItem item) {
        // Use the measured hue when it is reliable, else the canonical hue of the class.
        return item.lab().chroma() > 12 ? item.lab().hue() : item.color().lab().hue();
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
