package com.ryvenca.garment;

import java.util.List;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.ColorName;
import com.ryvenca.i18n.Localizer;

public record GarmentFilter(List<Category> categories, List<ColorName> colors, List<Season> seasons,
                            List<Occasion> occasions, Boolean favorite, String query) {

    public boolean matches(Garment g, Localizer l) {
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
            String q = query.trim().toLowerCase(l.locale());
            String haystack = String.join(" ", GarmentMapper.displayName(g, l), l.label(g.getSubcategory()),
                    l.label(g.getCategory()), l.label(g.getColor())).toLowerCase(l.locale());
            return haystack.contains(q);
        }
        return true;
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
