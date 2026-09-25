package com.ryvenca.outfit.engine;

import java.util.List;

import com.ryvenca.color.ColorName;

/**
 * @param score        0..1 combined color compatibility
 * @param paletteIndex curated palette the outfit's own colors literally come from (-1 if none)
 * @param paletteScore 0..1 best palette fit (neutrals get an allowance), used for scoring
 * @param paletteMatched whether {@link #paletteIndex} is worth naming
 * @param colors       distinct colors ordered by visual area
 * @param neutrals     neutral colors among the core pieces
 * @param accents      accent/soft colors among the core pieces
 */
public record ColorAnalysis(double score, int paletteIndex, double paletteScore, boolean paletteMatched, double ruleScore,
                            ColorCase colorCase, List<ColorName> colors, List<ColorName> neutrals,
                            List<ColorName> accents, boolean nearClash, boolean patternClash,
                            boolean bagShoeEcho, boolean accentEcho) {

    public enum ColorCase {
        SINGLE_COLOR, MONOCHROME, ALL_NEUTRAL, TONAL, SINGLE_ACCENT, SOFT_PAIR, ANALOGOUS, COMPLEMENTARY,
        DISCORDANT, BUSY
    }

    public boolean allNeutral() {
        return accents.isEmpty();
    }
}
