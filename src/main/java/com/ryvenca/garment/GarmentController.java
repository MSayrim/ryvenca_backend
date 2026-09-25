package com.ryvenca.garment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.auth.CurrentUser;
import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.ColorName;
import com.ryvenca.garment.GarmentDtos.FavoriteRequest;
import com.ryvenca.garment.GarmentDtos.GarmentDto;
import com.ryvenca.garment.GarmentDtos.GarmentRequest;
import com.ryvenca.user.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/garments")
public class GarmentController {

    private final GarmentService garments;
    private final GarmentMapper mapper;
    private final UserService users;

    public GarmentController(GarmentService garments, GarmentMapper mapper, UserService users) {
        this.garments = garments;
        this.mapper = mapper;
        this.users = users;
    }

    @GetMapping
    public List<GarmentDto> list(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(required = false) List<Category> category,
                                 @RequestParam(required = false) List<ColorName> color,
                                 @RequestParam(required = false) List<Season> season,
                                 @RequestParam(required = false) List<Occasion> occasion,
                                 @RequestParam(required = false) Boolean favorite,
                                 @RequestParam(required = false) String q) {
        GarmentFilter filter = new GarmentFilter(category, color, season, occasion, favorite, q);
        return garments.list(userId(jwt), filter).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public GarmentDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return mapper.toDto(garments.get(userId(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GarmentDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GarmentRequest request) {
        return mapper.toDto(garments.create(userId(jwt), request));
    }

    @PutMapping("/{id}")
    public GarmentDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                             @Valid @RequestBody GarmentRequest request) {
        return mapper.toDto(garments.update(userId(jwt), id, request));
    }

    @PutMapping("/{id}/favorite")
    public GarmentDto favorite(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                               @Valid @RequestBody FavoriteRequest request) {
        return mapper.toDto(garments.setFavorite(userId(jwt), id, request.favorite()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        garments.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private long userId(Jwt jwt) {
        return users.require(CurrentUser.id(jwt)).getId();
    }
}
