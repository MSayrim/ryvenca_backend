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
            @Size(max = 80, message = "İsim en fazla 80 karakter olabilir") String name,
            @NotNull(message = "Kategori seç") Category category,
            @NotNull(message = "Alt kategori seç") Subcategory subcategory,
            @NotNull(message = "Renk seç") ColorName color,
            String colorHex,
            Boolean pattern,
            List<Season> seasons,
            List<Occasion> occasions) {
    }

    public record FavoriteRequest(@NotNull(message = "favorite gerekli") Boolean favorite) {
    }
}
