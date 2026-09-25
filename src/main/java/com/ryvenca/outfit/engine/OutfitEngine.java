package com.ryvenca.outfit.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.PaletteLibrary;

/**
 * Builds outfits from a user's own wardrobe.
 *
 * <p>Search: season/occasion aware pools per role → best top×bottom pairs (pruned) and dresses →
 * × shoes (fully scored) → enrichment with optional outer layer, bag and accessory when they help →
 * seeded jitter for variety → diversity-aware greedy selection so suggestions do not repeat the same
 * pieces.
 */
public final class OutfitEngine {

    private static final int PAIR_BEAM = 40;
    private static final int BASE_BEAM = 140;
    private static final double JITTER = 2.5;
    /** "Bununla ne gider?" lists the essential roles first, then alternatives and finishing touches. */
    private static final List<OutfitRole> PAIRING_ORDER = List.of(OutfitRole.TOP, OutfitRole.BOTTOM, OutfitRole.SHOES,
            OutfitRole.DRESS, OutfitRole.OUTERWEAR, OutfitRole.BAG, OutfitRole.ACCESSORY);

    private final List<WardrobeItem> wardrobe;
    private final Map<Long, WardrobeItem> byId = new HashMap<>();
    private final OutfitScorer scorer;

    public OutfitEngine(List<WardrobeItem> wardrobe, PaletteLibrary palettes) {
        this.wardrobe = List.copyOf(wardrobe);
        this.wardrobe.forEach(i -> byId.put(i.id(), i));
        this.scorer = new OutfitScorer(new PaletteFit(palettes, this.wardrobe));
    }

    public OutfitScorer scorer() {
        return scorer;
    }

    public WardrobeItem item(long id) {
        return byId.get(id);
    }

    public OutfitEvaluation evaluate(List<WardrobeItem> items, OutfitRequest request) {
        return scorer.evaluate(items, request);
    }

    // ---- Suggestions ------------------------------------------------------------------------------

    public List<OutfitEvaluation> suggest(OutfitRequest request, int limit) {
        List<OutfitEvaluation> candidates = candidates(request, null, BASE_BEAM);
        return diversify(candidates, request.seed(), limit, null);
    }

    /** Number of distinct base outfits (top+bottom/dress+shoes) scoring at least {@code threshold}. */
    public int countReady(OutfitRequest request, int threshold, int cap) {
        Map<OutfitRole, List<WardrobeItem>> pools = pools(request, null);
        int count = 0;
        for (List<WardrobeItem> base : bases(pools, Integer.MAX_VALUE)) {
            for (WardrobeItem shoes : pools.get(OutfitRole.SHOES)) {
                List<WardrobeItem> items = new ArrayList<>(base);
                items.add(shoes);
                if (scorer.evaluate(items, request).score() >= threshold && ++count >= cap) {
                    return count;
                }
            }
        }
        return count;
    }

    // ---- "Bununla ne gider?" ----------------------------------------------------------------------

    public Pairings pairings(WardrobeItem anchor, OutfitRequest request, int perRole, int outfitLimit) {
        List<OutfitEvaluation> candidates = candidates(request, anchor, 220);
        Map<OutfitRole, Map<Long, Integer>> best = new EnumMap<>(OutfitRole.class);
        for (OutfitEvaluation e : candidates) {
            for (WardrobeItem item : e.items()) {
                if (item.id() != anchor.id()) {
                    best.computeIfAbsent(item.role(), r -> new HashMap<>()).merge(item.id(), e.score(), Math::max);
                }
            }
        }
        // Optional roles only appear when they improved an outfit; score every candidate piece on the
        // best few bases so the user sees all options.
        List<OutfitEvaluation> topBases = candidates.stream().limit(6).toList();
        for (OutfitRole role : List.of(OutfitRole.OUTERWEAR, OutfitRole.BAG, OutfitRole.ACCESSORY)) {
            if (role == anchor.role()) {
                continue;
            }
            for (WardrobeItem item : pool(request, role, null)) {
                for (OutfitEvaluation base : topBases) {
                    List<WardrobeItem> items = new ArrayList<>(base.items());
                    items.removeIf(i -> i.role() == role);
                    items.add(item);
                    if (OutfitScorer.structureProblem(items) == null) {
                        int score = scorer.evaluate(items, request).score();
                        best.computeIfAbsent(role, r -> new HashMap<>()).merge(item.id(), score, Math::max);
                    }
                }
            }
        }
        List<Pairings.RoleMatches> matches = new ArrayList<>();
        for (OutfitRole role : PAIRING_ORDER) {
            Map<Long, Integer> scores = best.get(role);
            if (role == anchor.role() || scores == null || scores.isEmpty()) {
                continue;
            }
            List<Pairings.Match> ranked = scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .limit(perRole)
                    .map(en -> new Pairings.Match(byId.get(en.getKey()), en.getValue()))
                    .toList();
            matches.add(new Pairings.RoleMatches(role, ranked));
        }
        return new Pairings(anchor, matches, diversify(candidates, request.seed(), outfitLimit, anchor));
    }

    // ---- Similar outfits --------------------------------------------------------------------------

    public List<OutfitEvaluation> similar(OutfitEvaluation reference, OutfitRequest request, int limit) {
        Map<String, OutfitEvaluation> pool = new LinkedHashMap<>();
        OutfitRequest broad = new OutfitRequest(request.season(), null, request.styles(), request.seed());
        candidates(broad, null, BASE_BEAM).forEach(e -> pool.putIfAbsent(e.key(), e));
        for (WardrobeItem item : reference.items()) {
            if (item.role().isCore() && item.role() != OutfitRole.SHOES) {
                candidates(broad, item, 40).forEach(e -> pool.putIfAbsent(e.key(), e));
            }
        }
        pool.remove(reference.key());
        Set<Long> refCore = coreIds(reference);
        Set<Object> refColors = new HashSet<>(reference.color().colors());
        List<OutfitEvaluation> ranked = new ArrayList<>(pool.values());
        Map<String, Double> similarity = new HashMap<>();
        for (OutfitEvaluation e : ranked) {
            Set<Long> core = coreIds(e);
            Set<Long> union = new HashSet<>(core);
            union.addAll(refCore);
            Set<Long> shared = new HashSet<>(core);
            shared.retainAll(refCore);
            double jaccard = union.isEmpty() ? 0 : shared.size() / (double) union.size();
            long sharedColors = e.color().colors().stream().filter(refColors::contains).count();
            double colorSim = sharedColors / (double) Math.max(1, Math.max(refColors.size(), e.color().colors().size()));
            double sim = 0.35 * jaccard
                    + 0.25 * (e.style().style() == reference.style().style() ? 1 : 0)
                    + 0.2 * (e.occasion().primary() == reference.occasion().primary() ? 1 : 0)
                    + 0.2 * colorSim;
            similarity.put(e.key(), 0.6 * sim + 0.4 * e.raw());
        }
        ranked.sort(Comparator.comparingDouble((OutfitEvaluation e) -> similarity.get(e.key())).reversed());
        List<OutfitEvaluation> result = new ArrayList<>();
        for (OutfitEvaluation e : ranked) {
            if (result.size() >= limit) {
                break;
            }
            boolean repetitive = result.stream().anyMatch(r -> sharedCore(r, e) >= coreIds(e).size() - 1);
            if (!repetitive || ranked.size() < limit * 2) {
                result.add(e);
            }
        }
        return result;
    }

    // ---- Candidate search -------------------------------------------------------------------------

    private List<OutfitEvaluation> candidates(OutfitRequest request, WardrobeItem anchor, int beam) {
        Map<OutfitRole, List<WardrobeItem>> pools = pools(request, anchor);
        List<List<WardrobeItem>> bases = bases(pools, PAIR_BEAM);
        List<OutfitEvaluation> scored = new ArrayList<>();
        for (List<WardrobeItem> base : bases) {
            for (WardrobeItem shoes : pools.get(OutfitRole.SHOES)) {
                List<WardrobeItem> items = new ArrayList<>(base);
                items.add(shoes);
                if (anchor != null && isLayer(anchor.role())) {
                    items.add(anchor);
                }
                scored.add(scorer.evaluate(items, request));
            }
        }
        scored.sort(Comparator.comparingDouble(OutfitEvaluation::raw).reversed());
        List<OutfitEvaluation> enriched = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OutfitEvaluation base : scored.subList(0, Math.min(beam, scored.size()))) {
            for (OutfitEvaluation e : enrich(base, pools, request)) {
                if (seen.add(e.key())) {
                    enriched.add(e);
                }
            }
        }
        enriched.sort(Comparator.comparingDouble(OutfitEvaluation::raw).reversed());
        return enriched;
    }

    /**
     * Completes a base outfit with an outer layer, bag and accessory when they help. Several layer
     * variants are returned (best, runner-up, none) so the diversity step can avoid putting the same
     * coat on every suggestion.
     */
    private List<OutfitEvaluation> enrich(OutfitEvaluation base, Map<OutfitRole, List<WardrobeItem>> pools,
                                          OutfitRequest request) {
        List<OutfitEvaluation> layered = new ArrayList<>();
        if (base.item(OutfitRole.OUTERWEAR) != null) {
            layered.add(base);
        } else {
            boolean cold = request.season() == Season.AUTUMN || request.season() == Season.WINTER;
            List<OutfitEvaluation> options = new ArrayList<>();
            for (WardrobeItem layer : pools.get(OutfitRole.OUTERWEAR)) {
                List<WardrobeItem> items = new ArrayList<>(base.items());
                items.add(layer);
                options.add(scorer.evaluate(items, request));
            }
            options.sort(Comparator.comparingDouble(OutfitEvaluation::raw).reversed());
            for (int i = 0; i < Math.min(2, options.size()); i++) {
                double gain = options.get(i).raw() - base.raw();
                boolean close = i == 0 || options.get(i).raw() >= options.getFirst().raw() - 0.02;
                if (close && ((cold && gain > -0.03) || gain > 0.005)) {
                    layered.add(options.get(i));
                }
            }
            if (layered.isEmpty() || request.season() != Season.WINTER) {
                layered.add(base);
            }
        }
        List<OutfitEvaluation> result = new ArrayList<>();
        for (OutfitEvaluation current : layered) {
            if (current.item(OutfitRole.BAG) == null) {
                OutfitEvaluation withBag = bestWith(current, pools.get(OutfitRole.BAG), request);
                if (withBag != null && withBag.raw() - current.raw() > -0.01) {
                    current = withBag;
                }
            }
            if (current.items().stream().noneMatch(i -> i.role() == OutfitRole.ACCESSORY)) {
                OutfitEvaluation withAccessory = bestWith(current, pools.get(OutfitRole.ACCESSORY), request);
                if (withAccessory != null && withAccessory.raw() - current.raw() > -0.005) {
                    current = withAccessory;
                }
            }
            result.add(current);
        }
        return result;
    }

    private OutfitEvaluation bestWith(OutfitEvaluation current, List<WardrobeItem> options, OutfitRequest request) {
        OutfitEvaluation best = null;
        for (WardrobeItem option : options) {
            List<WardrobeItem> items = new ArrayList<>(current.items());
            items.add(option);
            OutfitEvaluation e = scorer.evaluate(items, request);
            if (best == null || e.raw() > best.raw()) {
                best = e;
            }
        }
        return best;
    }

    private List<List<WardrobeItem>> bases(Map<OutfitRole, List<WardrobeItem>> pools, int pairBeam) {
        record Pair(List<WardrobeItem> items, double score) {
        }
        List<Pair> pairs = new ArrayList<>();
        for (WardrobeItem top : pools.get(OutfitRole.TOP)) {
            for (WardrobeItem bottom : pools.get(OutfitRole.BOTTOM)) {
                List<WardrobeItem> items = List.of(top, bottom);
                pairs.add(new Pair(items, pairBeam == Integer.MAX_VALUE ? 0 : scorer.pairScore(items)));
            }
        }
        pairs.sort(Comparator.comparingDouble(Pair::score).reversed());
        List<List<WardrobeItem>> bases = new ArrayList<>();
        // Keep the best pairs, but make sure every top and bottom gets a chance to appear.
        Set<Long> covered = new HashSet<>();
        for (int i = 0; i < pairs.size(); i++) {
            Pair pair = pairs.get(i);
            boolean fresh = !covered.contains(pair.items().get(0).id()) || !covered.contains(pair.items().get(1).id());
            if (i < pairBeam || fresh) {
                bases.add(pair.items());
                pair.items().forEach(it -> covered.add(it.id()));
            }
        }
        for (WardrobeItem dress : pools.get(OutfitRole.DRESS)) {
            bases.add(List.of(dress));
        }
        return bases;
    }

    // ---- Pools ------------------------------------------------------------------------------------

    private Map<OutfitRole, List<WardrobeItem>> pools(OutfitRequest request, WardrobeItem anchor) {
        Map<OutfitRole, List<WardrobeItem>> pools = new EnumMap<>(OutfitRole.class);
        for (OutfitRole role : OutfitRole.values()) {
            pools.put(role, pool(request, role, anchor));
        }
        if (anchor != null) {
            switch (anchor.role()) {
                case TOP, BOTTOM -> {
                    pools.put(anchor.role(), List.of(anchor));
                    pools.put(OutfitRole.DRESS, List.of());
                }
                case DRESS -> {
                    pools.put(OutfitRole.DRESS, List.of(anchor));
                    pools.put(OutfitRole.TOP, List.of());
                    pools.put(OutfitRole.BOTTOM, List.of());
                }
                case SHOES -> pools.put(OutfitRole.SHOES, List.of(anchor));
                case OUTERWEAR, BAG, ACCESSORY -> pools.put(anchor.role(), List.of());
            }
        }
        return pools;
    }

    private List<WardrobeItem> pool(OutfitRequest request, OutfitRole role, WardrobeItem anchor) {
        List<WardrobeItem> all = wardrobe.stream()
                .filter(i -> i.role() == role && (anchor == null || i.id() != anchor.id()))
                .toList();
        Season season = request.season();
        Predicate<WardrobeItem> allowed = i -> !blocked(i, season);
        List<WardrobeItem> inSeason = all.stream().filter(allowed).filter(i -> i.fits(season)).toList();
        List<WardrobeItem> result;
        if (!inSeason.isEmpty()) {
            result = inSeason;
        } else if (!role.isCore() || role == OutfitRole.OUTERWEAR) {
            result = all.stream().filter(allowed).filter(i -> i.fits(season)).toList();
        } else {
            List<WardrobeItem> notBlocked = all.stream().filter(allowed).toList();
            result = notBlocked.isEmpty() ? all : notBlocked;
        }
        Occasion occasion = request.occasion();
        if (occasion != null && role != OutfitRole.ACCESSORY) {
            List<WardrobeItem> tagged = result.stream().filter(i -> i.suits(occasion)
                    || (i.formality() >= occasion.minFormality() - 0.5 && i.formality() <= occasion.maxFormality() + 0.5))
                    .toList();
            if (!tagged.isEmpty() || !role.isCore() || role == OutfitRole.OUTERWEAR) {
                result = tagged;
            }
        }
        return result;
    }

    /** Roles that are added on top of a base outfit rather than forming it. */
    private static boolean isLayer(OutfitRole role) {
        return role == OutfitRole.OUTERWEAR || role == OutfitRole.BAG || role == OutfitRole.ACCESSORY;
    }

    /** Pieces that make no sense in a season whatever the user tagged (e.g. a puffer in summer). */
    private static boolean blocked(WardrobeItem item, Season season) {
        int warmth = item.subcategory().warmth();
        return switch (season) {
            case SUMMER -> warmth >= 3 || (item.role() == OutfitRole.TOP && warmth >= 2);
            case WINTER -> item.subcategory() == com.ryvenca.catalog.Subcategory.SANDAL
                    || item.subcategory() == com.ryvenca.catalog.Subcategory.SHORTS
                    || item.subcategory() == com.ryvenca.catalog.Subcategory.TANK_TOP;
            default -> false;
        };
    }

    // ---- Diversity --------------------------------------------------------------------------------

    private List<OutfitEvaluation> diversify(List<OutfitEvaluation> candidates, long seed, int limit,
                                             WardrobeItem anchor) {
        Random random = new Random(seed);
        Map<String, Double> jitter = new HashMap<>();
        for (OutfitEvaluation e : candidates) {
            jitter.put(e.key(), (random.nextDouble() * 2 - 1) * JITTER);
        }
        List<OutfitEvaluation> remaining = new ArrayList<>(candidates);
        List<OutfitEvaluation> selected = new ArrayList<>();
        Map<Long, Integer> usage = new HashMap<>();
        while (selected.size() < limit && !remaining.isEmpty()) {
            OutfitEvaluation best = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (OutfitEvaluation e : remaining) {
                double value = e.raw() * 100 + jitter.get(e.key());
                double maxShared = 0;
                for (OutfitEvaluation s : selected) {
                    maxShared = Math.max(maxShared, weightedShared(s, e, anchor));
                    if (sameBase(s, e)) {
                        value -= 18;
                    }
                }
                value -= 7 * maxShared;
                for (WardrobeItem item : e.items()) {
                    if (anchor == null || item.id() != anchor.id()) {
                        value -= usage.getOrDefault(item.id(), 0) * reusePenalty(item.role());
                    }
                }
                if (value > bestValue) {
                    bestValue = value;
                    best = e;
                }
            }
            selected.add(best);
            remaining.remove(best);
            best.items().forEach(i -> usage.merge(i.id(), 1, Integer::sum));
        }
        return selected;
    }

    /** Repeating a coat on every suggestion is far more noticeable than repeating the only pair of shoes. */
    private static double reusePenalty(OutfitRole role) {
        return switch (role) {
            case OUTERWEAR -> 3.0;
            case TOP, BOTTOM, DRESS -> 2.0;
            case BAG, ACCESSORY -> 1.0;
            case SHOES -> 0.75;
        };
    }

    private static double weightedShared(OutfitEvaluation a, OutfitEvaluation b, WardrobeItem anchor) {
        Set<Long> ids = new HashSet<>(a.ids());
        double shared = 0;
        for (WardrobeItem item : b.items()) {
            if (!ids.contains(item.id()) || (anchor != null && item.id() == anchor.id())) {
                continue;
            }
            shared += switch (item.role()) {
                case SHOES -> 0.5;
                case BAG, ACCESSORY -> 0.3;
                default -> 1.0;
            };
        }
        return shared;
    }

    private static boolean sameBase(OutfitEvaluation a, OutfitEvaluation b) {
        return baseKey(a).equals(baseKey(b));
    }

    private static List<Long> baseKey(OutfitEvaluation e) {
        return e.items().stream()
                .filter(i -> i.role() == OutfitRole.TOP || i.role() == OutfitRole.BOTTOM || i.role() == OutfitRole.DRESS)
                .map(WardrobeItem::id)
                .sorted()
                .toList();
    }

    private static Set<Long> coreIds(OutfitEvaluation e) {
        Set<Long> ids = new HashSet<>();
        e.items().stream().filter(i -> i.role().isCore()).forEach(i -> ids.add(i.id()));
        return ids;
    }

    private static long sharedCore(OutfitEvaluation a, OutfitEvaluation b) {
        Set<Long> ids = coreIds(a);
        return coreIds(b).stream().filter(ids::contains).count();
    }
}
