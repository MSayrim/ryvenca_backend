package com.ryvenca.garment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.Subcategory;
import com.ryvenca.color.ColorName;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class GarmentDtos {

    private GarmentDtos() {
    }

    public record GarmentDto(Long id, String name, String displayName, Category category, Subcategory subcategory,
                             ColorName color, String colorHex, ColorSource colorSource, boolean pattern,
                             List<Season> seasons, List<Occasion> occasions, boolean favorite, UUID imageId,
                             String imageUrl, String thumbnailUrl, Instant createdAt, Instant updatedAt) {
    }

    public record GarmentRequest(
            UUID imageId,
            @Size(max = 80, message = "{validation.name.size}") String name,
            @NotNull(message = "{validation.category.required}") Category category,
            @NotNull(message = "{validation.subcategory.required}") Subcategory subcategory,
            @NotNull(message = "{validation.color.required}") ColorName color,
            String colorHex,
            Boolean pattern,
            List<Season> seasons,
            List<Occasion> occasions) {
    }

    public record FavoriteRequest(@NotNull(message = "{validation.favorite.required}") Boolean favorite) {
    }
}
