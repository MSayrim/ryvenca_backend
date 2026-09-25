package com.ryvenca.outfit.engine;

import com.ryvenca.catalog.Season;

/**
 * @param score   0..1
 * @param season  season the outfit was scored for
 * @param warmth  TOO_LIGHT / TOO_WARM when the outfit does not suit the weather, else OK
 * @param layer   the outer layer that makes the outfit weather-proof, if any
 */
public record SeasonAnalysis(double score, Season season, Warmth warmth, WardrobeItem layer) {

    public enum Warmth {
        OK, TOO_LIGHT, TOO_WARM
    }
}
