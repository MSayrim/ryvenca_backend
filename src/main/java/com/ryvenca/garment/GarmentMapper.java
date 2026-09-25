package com.ryvenca.garment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ryvenca.garment.GarmentDtos.GarmentDto;
import com.ryvenca.i18n.Localizer;
import com.ryvenca.i18n.Texts;
import com.ryvenca.image.ImageService;

@Component
public class GarmentMapper {

    private final ImageService images;
    private final Texts texts;

    public GarmentMapper(ImageService images, Texts texts) {
        this.images = images;
        this.texts = texts;
    }

    public GarmentDto toDto(Garment g) {
        return new GarmentDto(g.getId(), g.getName(), displayName(g, texts.current()), g.getCategory(), g.getSubcategory(), g.getColor(),
                g.getColorHex(), g.getColorSource(), g.isPattern(), List.copyOf(g.getSeasons()),
                List.copyOf(g.getOccasions()), g.isFavorite(), g.getImage().getId(), images.url(g.getImage()),
                images.thumbnailUrl(g.getImage()), g.getCreatedAt(), g.getUpdatedAt());
    }

    /** The user's own name, or a generated one such as "Beige blazer" / "Bej Blazer" in the request language. */
    public static String displayName(Garment g, Localizer l) {
        if (g.getName() != null && !g.getName().isBlank()) {
            return g.getName().trim();
        }
        // {0}/{1}: color and garment labels as written standalone, {2}/{3}: as written inside a sentence.
        return l.t("garment.generatedName", l.label(g.getColor()), l.label(g.getSubcategory()), l.inline(g.getColor()),
                l.inline(g.getSubcategory()));
    }
}
