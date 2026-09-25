package com.ryvenca.garment;

import java.util.List;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.ColorName;
import com.ryvenca.common.TurkishText;

public record GarmentFilter(List<Category> categories, List<ColorName> colors, List<Season> seasons,
                            List<Occasion> occasions, Boolean favorite, String query) {

    public boolean matches(Garment g) {
        if (notEmpty(categories) && !categories.contains(g.getCategory())) {
            return false;
        }
        if (notEmpty(colors) && !colors.contains(g.getColor())) {
            return false;
        }
        if (notEmpty(seasons) && seasons.stream().noneMatch(g.getSeasons()::contains)) {
            return false;
        }
        if (notEmpty(occasions) && occasions.stream().noneMatch(g.getOccasions()::contains)) {
            return false;
        }
        if (favorite != null && g.isFavorite() != favorite) {
            return false;
        }
        if (query != null && !query.isBlank()) {
            String q = TurkishText.lower(query.trim());
            String haystack = TurkishText.lower(String.join(" ", GarmentMapper.displayName(g),
                    g.getSubcategory().label(), g.getCategory().label(), g.getColor().label()));
            return haystack.contains(q);
        }
        return true;
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
