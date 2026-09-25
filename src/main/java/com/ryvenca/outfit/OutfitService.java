package com.ryvenca.outfit;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.ColorName;
import com.ryvenca.color.Lab;
import com.ryvenca.color.PaletteLibrary;
import com.ryvenca.common.ApiException;
import com.ryvenca.garment.Garment;
import com.ryvenca.garment.GarmentMapper;
import com.ryvenca.garment.GarmentService;
import com.ryvenca.outfit.OutfitDtos.MissingPiece;
import com.ryvenca.outfit.OutfitDtos.OutfitDto;
import com.ryvenca.outfit.OutfitDtos.OutfitItem;
import com.ryvenca.outfit.OutfitDtos.PairingItem;
import com.ryvenca.outfit.OutfitDtos.PairingsResponse;
import com.ryvenca.outfit.OutfitDtos.PaletteColor;
import com.ryvenca.outfit.OutfitDtos.Readiness;
import com.ryvenca.outfit.OutfitDtos.Reason;
import com.ryvenca.outfit.OutfitDtos.RoleMatches;
import com.ryvenca.outfit.OutfitDtos.SaveOutfitRequest;
import com.ryvenca.outfit.OutfitDtos.ScorePart;
import com.ryvenca.outfit.OutfitDtos.SimilarResponse;
import com.ryvenca.outfit.OutfitDtos.SuggestionsResponse;
import com.ryvenca.outfit.engine.OutfitEngine;
import com.ryvenca.outfit.engine.OutfitEvaluation;
import com.ryvenca.outfit.engine.OutfitExplainer;
import com.ryvenca.outfit.engine.OutfitRequest;
import com.ryvenca.outfit.engine.OutfitScorer;
import com.ryvenca.outfit.engine.OutfitStory;
import com.ryvenca.outfit.engine.Pairings;
import com.ryvenca.outfit.engine.WardrobeItem;
import com.ryvenca.user.User;
import com.ryvenca.user.UserService;

@Service
public class OutfitService {

    public static final int RECOMMENDED_MINIMUM = 8;
    public static final int READY_THRESHOLD = 70;
    public static final int READY_CAP = 99;

    private final UserService users;
    private final GarmentService garments;
    private final GarmentMapper mapper;
    private final SavedOutfitRepository savedOutfits;
    private final PaletteLibrary palettes;
    private final OutfitExplainer explainer;
    private final Clock clock;

    public OutfitService(UserService users, GarmentService garments, GarmentMapper mapper,
                         SavedOutfitRepository savedOutfits, PaletteLibrary palettes, Clock clock) {
        this.users = users;
        this.garments = garments;
        this.mapper = mapper;
        this.savedOutfits = savedOutfits;
        this.palettes = palettes;
        this.explainer = new OutfitExplainer(palettes);
        this.clock = clock;
    }

    /** Everything needed to answer one outfit request for one user. */
    public record Context(User user, Map<Long, Garment> garments, OutfitEngine engine, Map<String, Long> savedKeys) {

        public List<Garment> all() {
            return List.copyOf(garments.values());
        }
    }

    @Transactional(readOnly = true)
    public Context context(long userId) {
        User user = users.require(userId);
        Map<Long, Garment> byId = garments.wardrobe(userId).stream()
                .collect(Collectors.toMap(Garment::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<WardrobeItem> items = byId.values().stream().map(OutfitService::toItem).toList();
        Map<String, Long> saved = new HashMap<>();
        savedOutfits.findByOwnerIdOrderByCreatedAtDesc(userId).forEach(s -> saved.put(s.getOutfitKey(), s.getId()));
        return new Context(user, byId, new OutfitEngine(items, palettes), saved);
    }

    public Season currentSeason() {
        return Season.of(LocalDate.now(clock));
    }

    OutfitRequest request(Context ctx, Season season, Occasion occasion, Long seed) {
        long effectiveSeed = seed != null ? seed : LocalDate.now(clock).toEpochDay() * 1_000_003L + ctx.user().getId();
        return new OutfitRequest(season != null ? season : currentSeason(), occasion,
                ctx.user().getStylePreferences(), effectiveSeed);
    }

    @Transactional(readOnly = true)
    public SuggestionsResponse suggestions(long userId, Occasion occasion, Season season, int limit, Long seed) {
        Context ctx = context(userId);
        OutfitRequest request = request(ctx, season, occasion, seed);
        Readiness readiness = readiness(ctx.all());
        List<OutfitDto> outfits = readiness.ready()
                ? toDtos(ctx.engine().suggest(request, limit), ctx)
                : List.of();
        return new SuggestionsResponse(outfits, readiness, request.season(), occasion);
    }

    @Transactional(readOnly = true)
    public OutfitDto evaluate(long userId, List<Long> ids, Season season) {
        Context ctx = context(userId);
        return toDto(evaluate(ctx, ids, request(ctx, season, null, null)), ctx);
    }

    @Transactional(readOnly = true)
    public SimilarResponse similar(long userId, List<Long> ids, int limit) {
        Context ctx = context(userId);
        OutfitRequest request = request(ctx, null, null, null);
        OutfitEvaluation reference = evaluate(ctx, ids, request);
        return new SimilarResponse(toDtos(ctx.engine().similar(reference, request, limit), ctx));
    }

    @Transactional(readOnly = true)
    public PairingsResponse pairings(long userId, long garmentId, Season season, Occasion occasion) {
        Context ctx = context(userId);
        Garment anchor = ctx.garments().get(garmentId);
        if (anchor == null) {
            throw ApiException.notFound("Parça bulunamadı.");
        }
        Readiness readiness = readiness(ctx.all());
        OutfitRequest request = request(ctx, season, occasion, null);
        Pairings pairings = ctx.engine().pairings(ctx.engine().item(garmentId), request, 6, 6);
        List<RoleMatches> matches = pairings.matches().stream()
                .map(m -> new RoleMatches(m.role(), m.role().label(), m.items().stream()
                        .map(match -> new PairingItem(mapper.toDto(ctx.garments().get(match.item().id())), match.score()))
                        .toList()))
                .toList();
        return new PairingsResponse(mapper.toDto(anchor), matches,
                toDtos(pairings.outfits(), ctx), readiness);
    }

    @Transactional(readOnly = true)
    public List<OutfitDto> saved(long userId) {
        Context ctx = context(userId);
        OutfitRequest request = request(ctx, null, null, null);
        List<OutfitDto> result = new ArrayList<>();
        for (SavedOutfit saved : savedOutfits.findByOwnerIdOrderByCreatedAtDesc(userId)) {
            List<WardrobeItem> items = new ArrayList<>();
            for (Long id : saved.garmentIds()) {
                WardrobeItem item = ctx.engine().item(id);
                if (item != null) {
                    items.add(item);
                }
            }
            if (items.size() != saved.garmentIds().size() || OutfitScorer.structureProblem(items) != null) {
                continue;
            }
            OutfitDto dto = toDto(ctx.engine().evaluate(items, request), ctx);
            result.add(saved.getTitle() == null ? dto : withTitle(dto, saved.getTitle()));
        }
        return result;
    }

    @Transactional
    public OutfitDto save(long userId, SaveOutfitRequest body) {
        Context ctx = context(userId);
        OutfitRequest request = request(ctx, null, null, null);
        OutfitEvaluation evaluation = evaluate(ctx, body.garmentIds(), request);
        String key = evaluation.key();
        SavedOutfit saved = savedOutfits.findByOwnerIdAndOutfitKey(userId, key).orElseGet(() -> {
            String title = body.title() == null || body.title().isBlank() ? null : body.title().trim();
            return savedOutfits.save(new SavedOutfit(userId, key, title));
        });
        ctx.savedKeys().put(key, saved.getId());
        OutfitDto dto = toDto(evaluation, ctx);
        return saved.getTitle() == null ? dto : withTitle(dto, saved.getTitle());
    }

    @Transactional
    public void unsave(long userId, long savedId) {
        users.require(userId);
        SavedOutfit saved = savedOutfits.findByIdAndOwnerId(savedId, userId)
                .orElseThrow(() -> ApiException.notFound("Kayıtlı kombin bulunamadı."));
        savedOutfits.delete(saved);
    }

    @Transactional(readOnly = true)
    public long savedCount(long userId) {
        return savedOutfits.countByOwnerId(userId);
    }

    @Transactional(readOnly = true)
    public int readyCount(Context ctx) {
        if (!readiness(ctx.all()).ready()) {
            return 0;
        }
        return ctx.engine().countReady(request(ctx, null, null, null), READY_THRESHOLD, READY_CAP);
    }

    public List<OutfitDto> todays(Context ctx, int limit) {
        if (!readiness(ctx.all()).ready()) {
            return List.of();
        }
        return toDtos(ctx.engine().suggest(request(ctx, null, null, null), limit), ctx);
    }

    public Readiness readiness(List<Garment> wardrobe) {
        Map<Category, Long> counts = wardrobe.stream()
                .collect(Collectors.groupingBy(Garment::getCategory, Collectors.counting()));
        long tops = counts.getOrDefault(Category.TOP, 0L);
        long bottoms = counts.getOrDefault(Category.BOTTOM, 0L);
        long dresses = counts.getOrDefault(Category.DRESS, 0L);
        long shoes = counts.getOrDefault(Category.SHOES, 0L);
        List<MissingPiece> missing = new ArrayList<>();
        if (dresses == 0) {
            if (tops == 0) {
                missing.add(new MissingPiece(Category.TOP, "Kombin önerebilmemiz için en az bir üst (ya da bir elbise) ekle."));
            }
            if (bottoms == 0) {
                missing.add(new MissingPiece(Category.BOTTOM, "Kombin önerebilmemiz için en az bir alt (ya da bir elbise) ekle."));
            }
        }
        if (shoes == 0) {
            missing.add(new MissingPiece(Category.SHOES, "Kombin önerebilmemiz için en az bir ayakkabı ekle."));
        }
        return new Readiness(missing.isEmpty(), wardrobe.size(), RECOMMENDED_MINIMUM, missing);
    }

    private OutfitEvaluation evaluate(Context ctx, List<Long> ids, OutfitRequest request) {
        if (ids == null || ids.isEmpty()) {
            throw ApiException.invalidField("items", "Kombin parçaları gerekli.");
        }
        List<WardrobeItem> items = new ArrayList<>();
        for (Long id : ids.stream().distinct().toList()) {
            WardrobeItem item = id == null ? null : ctx.engine().item(id);
            if (item == null) {
                throw ApiException.notFound("Kombindeki parçalardan biri bulunamadı.");
            }
            items.add(item);
        }
        String problem = OutfitScorer.structureProblem(items);
        if (problem != null) {
            throw ApiException.invalidField("items", problem);
        }
        return ctx.engine().evaluate(items, request);
    }

    /** Maps a list of outfits, giving cards in the same list distinct titles where possible. */
    List<OutfitDto> toDtos(List<OutfitEvaluation> evaluations, Context ctx) {
        Set<String> usedTitles = new HashSet<>();
        List<OutfitDto> result = new ArrayList<>();
        for (OutfitEvaluation e : evaluations) {
            OutfitDto dto = toDto(e, ctx, usedTitles);
            usedTitles.add(dto.title());
            result.add(dto);
        }
        return result;
    }

    OutfitDto toDto(OutfitEvaluation e, Context ctx) {
        return toDto(e, ctx, Set.of());
    }

    private OutfitDto toDto(OutfitEvaluation e, Context ctx, Set<String> usedTitles) {
        OutfitStory story = explainer.explain(e, usedTitles);
        List<ScorePart> breakdown = List.of(
                new ScorePart("COLOR", "Renk uyumu", 40, pct(e.color().score())),
                new ScorePart("CATEGORY", "Parça uyumu", 25, pct(e.compatibility().score())),
                new ScorePart("SEASON", "Mevsim", 15, pct(e.season().score())),
                new ScorePart("OCCASION", "Kullanım alanı", 15, pct(e.occasion().score())),
                new ScorePart("STYLE", "Stil", 5, pct(e.style().score())));
        List<OutfitItem> items = e.items().stream()
                .map(i -> new OutfitItem(i.role(), mapper.toDto(ctx.garments().get(i.id()))))
                .toList();
        List<PaletteColor> palette = new ArrayList<>();
        for (ColorName color : e.color().colors()) {
            String hex = e.items().stream()
                    .filter(i -> i.color() == color)
                    .max((a, b) -> Double.compare(a.role().visualArea(), b.role().visualArea()))
                    .map(i -> ctx.garments().get(i.id()).getColorHex())
                    .orElse(color.hex());
            palette.add(new PaletteColor(color, color.label(), hex));
        }
        List<Reason> reasons = story.reasons().stream().map(r -> new Reason(r.code(), r.title(), r.text())).toList();
        Long savedId = ctx.savedKeys().get(e.key());
        return new OutfitDto(e.key(), story.title(), story.description(), e.score(), breakdown, e.style().style(),
                e.style().style().label(), e.occasion().primary(), e.occasion().occasions(), e.occasion().venues(),
                items, palette, story.paletteName(), reasons, savedId != null, savedId);
    }

    private static OutfitDto withTitle(OutfitDto d, String title) {
        return new OutfitDto(d.key(), title, d.description(), d.score(), d.breakdown(), d.style(), d.styleLabel(),
                d.primaryOccasion(), d.occasions(), d.venues(), d.items(), d.palette(), d.paletteName(), d.reasons(),
                d.saved(), d.savedId());
    }

    private static int pct(double v) {
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 100);
    }

    static WardrobeItem toItem(Garment g) {
        return new WardrobeItem(g.getId(), g.getSubcategory(), g.getColor(), Lab.ofHex(g.getColorHex()), g.isPattern(),
                g.getSeasons(), g.getOccasions());
    }
}
