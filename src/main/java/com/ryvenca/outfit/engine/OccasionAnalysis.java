package com.ryvenca.outfit.engine;

import java.util.List;
import java.util.Map;

import com.ryvenca.catalog.Occasion;

/**
 * @param score     0..1 for the requested occasion (or the best one when none was requested)
 * @param primary   the occasion the outfit suits best
 * @param occasions occasions the outfit suits well, best first
 * @param venues    concrete places (office, dinner …)
 */
public record OccasionAnalysis(double score, Occasion requested, Occasion primary, List<Occasion> occasions,
                               List<Venue> venues, Map<Occasion, Double> scores, double formality) {
}
