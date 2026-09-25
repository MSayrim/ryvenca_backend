package com.ryvenca.outfit.engine;

import java.util.Map;

import com.ryvenca.catalog.StylePreference;

public record StyleAnalysis(double score, StylePreference style, Map<StylePreference, Double> affinity) {
}
