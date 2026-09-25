package com.ryvenca.home;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.auth.CurrentUser;
import com.ryvenca.catalog.Season;
import com.ryvenca.garment.GarmentDtos.GarmentDto;
import com.ryvenca.garment.GarmentMapper;
import com.ryvenca.garment.GarmentService;
import com.ryvenca.outfit.OutfitDtos.OutfitDto;
import com.ryvenca.outfit.OutfitDtos.Readiness;
import com.ryvenca.outfit.OutfitService;

@RestController
public class HomeController {

    public record Stats(long garmentCount, int readyOutfitCount, long favoriteCount) {
    }

    public record HomeResponse(Stats stats, List<GarmentDto> recentGarments, List<OutfitDto> todaysSuggestions,
                               Readiness readiness, Season season) {
    }

    private final OutfitService outfits;
    private final GarmentService garments;
    private final GarmentMapper mapper;

    public HomeController(OutfitService outfits, GarmentService garments, GarmentMapper mapper) {
        this.outfits = outfits;
        this.garments = garments;
        this.mapper = mapper;
    }

    @GetMapping("/api/home")
    @Transactional(readOnly = true)
    public HomeResponse home(@AuthenticationPrincipal Jwt jwt) {
        long userId = CurrentUser.id(jwt);
        var ctx = outfits.context(userId);
        var all = ctx.all();
        Readiness readiness = outfits.readiness(all);
        long favorites = garments.favoriteCount(userId) + outfits.savedCount(userId);
        Stats stats = new Stats(all.size(), outfits.readyCount(ctx), favorites);
        List<GarmentDto> recent = all.stream().limit(12).map(mapper::toDto).toList();
        return new HomeResponse(stats, recent, outfits.todays(ctx, 4), readiness, outfits.currentSeason());
    }
}
