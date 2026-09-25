package com.ryvenca.outfit;

import java.util.List;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.StylePreference;
import com.ryvenca.color.ColorName;
import com.ryvenca.garment.GarmentDtos.GarmentDto;
import com.ryvenca.outfit.engine.OutfitRole;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class OutfitDtos {

    private OutfitDtos() {
    }

    public record ScorePart(String code, String label, int weight, int score) {
    }

    public record OutfitItem(OutfitRole role, GarmentDto garment) {
    }

    public record PaletteColor(ColorName color, String label, String hex) {
    }

    public record Reason(String code, String title, String text) {
    }

    public record OutfitDto(String key, String title, String description, int score, List<ScorePart> breakdown,
                            StylePreference style, String styleLabel, Occasion primaryOccasion,
                            List<Occasion> occasions, List<String> venues, List<OutfitItem> items,
                            List<PaletteColor> palette, String paletteName, List<Reason> reasons, boolean saved,
                            Long savedId) {
    }

    public record MissingPiece(Category category, String message) {
    }

    public record Readiness(boolean ready, long garmentCount, int recommendedMinimum, List<MissingPiece> missing) {
    }

    public record SuggestionsResponse(List<OutfitDto> outfits, Readiness readiness, Season season, Occasion occasion) {
    }

    public record SimilarResponse(List<OutfitDto> outfits) {
    }

    public record PairingItem(GarmentDto garment, int score) {
    }

    public record RoleMatches(OutfitRole role, String label, List<PairingItem> items) {
    }

    public record PairingsResponse(GarmentDto anchor, List<RoleMatches> matches, List<OutfitDto> outfits,
                                   Readiness readiness) {
    }

    public record SaveOutfitRequest(
            @NotEmpty(message = "Kombin parçaları gerekli") @Size(max = 10, message = "Kombin en fazla 10 parça içerebilir")
            List<Long> garmentIds,
            @Size(max = 80, message = "Başlık en fazla 80 karakter olabilir") String title) {
    }
}
