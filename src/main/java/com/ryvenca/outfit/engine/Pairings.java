package com.ryvenca.outfit.engine;

import java.util.List;

/** "Bununla ne gider?" result for one anchor piece. */
public record Pairings(WardrobeItem anchor, List<RoleMatches> matches, List<OutfitEvaluation> outfits) {

    public record RoleMatches(OutfitRole role, List<Match> items) {
    }

    public record Match(WardrobeItem item, int score) {
    }
}
