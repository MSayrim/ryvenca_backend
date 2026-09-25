package com.ryvenca.outfit.engine;

import java.util.Set;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.Subcategory;
import com.ryvenca.color.ColorName;
import com.ryvenca.color.ColorTone;
import com.ryvenca.color.Lab;
import com.ryvenca.common.TurkishText;

/** Engine view of a garment: everything the scorer needs, nothing persistence related. */
public record WardrobeItem(long id, Subcategory subcategory, ColorName color, Lab lab, boolean pattern,
                           Set<Season> seasons, Set<Occasion> occasions) {

    public Category category() {
        return subcategory.category();
    }

    public OutfitRole role() {
        return OutfitRole.of(subcategory.category());
    }

    public double formality() {
        return subcategory.formality();
    }

    public boolean isDenim() {
        return subcategory == Subcategory.JEANS || subcategory == Subcategory.DENIM_JACKET;
    }

    public boolean fits(Season season) {
        return seasons.isEmpty() || seasons.contains(season);
    }

    public boolean suits(Occasion occasion) {
        return occasions.isEmpty() || occasions.contains(occasion);
    }

    /**
     * How the color behaves in an outfit. Denim and measured near-grays act as neutrals whatever
     * their class, because that is how they are worn.
     */
    public ColorTone tone() {
        if (isDenim() && (color == ColorName.BLUE || color == ColorName.LIGHT_BLUE || color == ColorName.NAVY
                || color == ColorName.GRAY || color == ColorName.BLACK)) {
            return ColorTone.NEUTRAL;
        }
        if (lab.chroma() < 9 && color != ColorName.PINK && color != ColorName.LIGHT_BLUE) {
            return ColorTone.NEUTRAL;
        }
        return color.tone();
    }

    /** Short lowercase description used inside sentences, e.g. "bej blazer". */
    public String phrase() {
        return TurkishText.lower(color.label() + " " + subcategory.label());
    }
}
