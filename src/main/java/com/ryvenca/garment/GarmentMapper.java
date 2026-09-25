package com.ryvenca.garment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ryvenca.garment.GarmentDtos.GarmentDto;
import com.ryvenca.image.ImageService;

@Component
public class GarmentMapper {

    private final ImageService images;

    public GarmentMapper(ImageService images) {
        this.images = images;
    }

    public GarmentDto toDto(Garment g) {
        return new GarmentDto(g.getId(), g.getName(), displayName(g), g.getCategory(), g.getSubcategory(), g.getColor(),
                g.getColorHex(), g.getColorSource(), g.isPattern(), List.copyOf(g.getSeasons()),
                List.copyOf(g.getOccasions()), g.isFavorite(), g.getImage().getId(), images.url(g.getImage()),
                images.thumbnailUrl(g.getImage()), g.getCreatedAt(), g.getUpdatedAt());
    }

    public static String displayName(Garment g) {
        if (g.getName() != null && !g.getName().isBlank()) {
            return g.getName().trim();
        }
        return g.getColor().label() + " " + g.getSubcategory().label();
    }
}
