package com.ryvenca.outfit.engine;

import java.util.Set;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.StylePreference;

/**
 * @param season     target season (never null)
 * @param occasion   requested occasion or null for "all"
 * @param styles     the user's style preferences (may be empty)
 * @param seed       controls tie-breaking jitter so "Yeniden öner" gives fresh but still good results
 */
public record OutfitRequest(Season season, Occasion occasion, Set<StylePreference> styles, long seed) {
}
