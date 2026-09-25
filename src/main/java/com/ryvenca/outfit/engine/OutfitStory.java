package com.ryvenca.outfit.engine;

import java.util.List;

/** The human side of an evaluation: name, one-line description and "Neden Uyumlu?" reasons. */
public record OutfitStory(String title, String description, List<Reason> reasons, String paletteName) {

    public record Reason(String code, String title, String text) {
    }
}
