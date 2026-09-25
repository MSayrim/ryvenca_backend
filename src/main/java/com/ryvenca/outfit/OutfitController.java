package com.ryvenca.outfit;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.auth.CurrentUser;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.outfit.OutfitDtos.OutfitDto;
import com.ryvenca.outfit.OutfitDtos.PairingsResponse;
import com.ryvenca.outfit.OutfitDtos.SaveOutfitRequest;
import com.ryvenca.outfit.OutfitDtos.SimilarResponse;
import com.ryvenca.outfit.OutfitDtos.SuggestionsResponse;

import jakarta.validation.Valid;

@RestController
public class OutfitController {

    private final OutfitService outfits;

    public OutfitController(OutfitService outfits) {
        this.outfits = outfits;
    }

    @GetMapping("/api/outfits/suggestions")
    public SuggestionsResponse suggestions(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) Occasion occasion,
                                           @RequestParam(required = false) Season season,
                                           @RequestParam(defaultValue = "10") int limit,
                                           @RequestParam(required = false) Long seed) {
        return outfits.suggestions(CurrentUser.id(jwt), occasion, season, Math.max(1, Math.min(limit, 20)), seed);
    }

    @GetMapping("/api/outfits/evaluate")
    public OutfitDto evaluate(@AuthenticationPrincipal Jwt jwt, @RequestParam List<Long> items,
                              @RequestParam(required = false) Season season) {
        return outfits.evaluate(CurrentUser.id(jwt), items, season);
    }

    @GetMapping("/api/outfits/similar")
    public SimilarResponse similar(@AuthenticationPrincipal Jwt jwt, @RequestParam List<Long> items,
                                   @RequestParam(defaultValue = "6") int limit) {
        return outfits.similar(CurrentUser.id(jwt), items, Math.max(1, Math.min(limit, 12)));
    }

    @GetMapping("/api/outfits/saved")
    public List<OutfitDto> saved(@AuthenticationPrincipal Jwt jwt) {
        return outfits.saved(CurrentUser.id(jwt));
    }

    @PostMapping("/api/outfits/saved")
    @ResponseStatus(HttpStatus.CREATED)
    public OutfitDto save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SaveOutfitRequest request) {
        return outfits.save(CurrentUser.id(jwt), request);
    }

    @DeleteMapping("/api/outfits/saved/{id}")
    public ResponseEntity<Void> unsave(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        outfits.unsave(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/garments/{id}/pairings")
    public PairingsResponse pairings(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                     @RequestParam(required = false) Season season,
                                     @RequestParam(required = false) Occasion occasion) {
        return outfits.pairings(CurrentUser.id(jwt), id, season, occasion);
    }
}
