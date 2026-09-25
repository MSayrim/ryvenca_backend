package com.ryvenca.outfit.engine;

import static com.ryvenca.catalog.Subcategory.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Subcategory;

/** Occasion compatibility from the pieces' occasion tags and the outfit's overall formality. */
final class OccasionFit {

    private static final Set<Subcategory> EVENING_SIGNALS = EnumSet.of(HEELS, CLUTCH, SLIP_DRESS, EVENING_DRESS, DRESS_SHOE);
    private static final Set<Subcategory> OFFICE_BREAKERS = EnumSet.of(HOODIE, SWEATPANTS, SHORTS, RUNNING_SHOE, TANK_TOP, SANDAL);
    private static final Set<Subcategory> TRAVEL_SHOES = EnumSet.of(SNEAKER, LOAFER, BOOT, FLATS);
    private static final Set<Subcategory> DRESSY = EnumSet.of(HEELS, CLUTCH, EVENING_DRESS, SLIP_DRESS);

    private OccasionFit() {
    }

    static OccasionAnalysis analyze(List<WardrobeItem> items, Occasion requested) {
        List<WardrobeItem> relevant = items.stream().filter(i -> i.role() != OutfitRole.ACCESSORY).toList();
        double formality = 0;
        double weights = 0;
        for (WardrobeItem item : relevant) {
            double w = item.role() == OutfitRole.BAG ? 0.5 : 1.0;
            formality += item.formality() * w;
            weights += w;
        }
        formality /= weights;

        Set<Subcategory> subs = EnumSet.noneOf(Subcategory.class);
        items.forEach(i -> subs.add(i.subcategory()));
        WardrobeItem shoes = ColorHarmony.find(items, OutfitRole.SHOES);

        Map<Occasion, Double> scores = new EnumMap<>(Occasion.class);
        for (Occasion occasion : Occasion.values()) {
            double tagged = 0;
            for (WardrobeItem item : relevant) {
                double w = item.role() == OutfitRole.BAG ? 0.5 : 1.0;
                tagged += item.suits(occasion) ? w : 0;
            }
            double tagFraction = tagged / weights;
            double distance = formality < occasion.minFormality() ? occasion.minFormality() - formality
                    : formality > occasion.maxFormality() ? formality - occasion.maxFormality() : 0;
            double formalityFit = 1 - Math.min(1, distance / 1.5);
            double s = 0.55 * tagFraction + 0.45 * formalityFit;
            switch (occasion) {
                case SPORT -> {
                    if (shoes == null || (shoes.subcategory() != RUNNING_SHOE && shoes.subcategory() != SNEAKER)) {
                        s *= 0.4;
                    }
                }
                case EVENING -> {
                    if (subs.stream().anyMatch(EVENING_SIGNALS::contains)) {
                        s += 0.08;
                    }
                }
                case OFFICE -> {
                    if (subs.stream().anyMatch(OFFICE_BREAKERS::contains)) {
                        s *= 0.6;
                    }
                }
                default -> {
                }
            }
            scores.put(occasion, Math.max(0, Math.min(1, s)));
        }

        List<Occasion> ranked = new ArrayList<>(List.of(Occasion.values()));
        ranked.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        Occasion primary = ranked.getFirst();
        // Secondary occasions must also be supported by the main garments themselves (a slip dress with
        // office-friendly heels is still not an office outfit).
        List<WardrobeItem> main = relevant.stream()
                .filter(i -> i.role() == OutfitRole.TOP || i.role() == OutfitRole.BOTTOM || i.role() == OutfitRole.DRESS)
                .toList();
        List<Occasion> suited = new ArrayList<>();
        for (Occasion occasion : ranked) {
            boolean mainAgrees = main.stream().allMatch(i -> i.suits(occasion));
            if (suited.isEmpty() || (scores.get(occasion) >= 0.72 && mainAgrees && suited.size() < 3)) {
                suited.add(occasion);
            }
        }
        if (requested != null && !suited.contains(requested) && scores.get(requested) >= 0.6) {
            suited.addFirst(requested);
        }
        double score = requested != null ? scores.get(requested) : scores.get(primary);
        return new OccasionAnalysis(score, requested, primary, List.copyOf(suited),
                venues(suited, formality, subs, shoes), scores, formality);
    }

    private static List<Venue> venues(List<Occasion> occasions, double formality, Set<Subcategory> subs,
                                       WardrobeItem shoes) {
        Set<Venue> venues = new LinkedHashSet<>();
        for (Occasion occasion : occasions) {
            switch (occasion) {
                case OFFICE -> {
                    venues.add(Venue.OFFICE);
                    if (formality >= 3.7) {
                        venues.add(Venue.MEETING);
                    }
                }
                case EVENING -> {
                    venues.add(Venue.DINNER);
                    if (formality >= 4.3 || subs.stream().anyMatch(DRESSY::contains)) {
                        venues.add(Venue.PARTY);
                    }
                }
                case DAILY -> {
                    venues.add(Venue.COFFEE);
                    if (formality < 3.2) {
                        venues.add(Venue.SHOPPING);
                    }
                }
                case WEEKEND -> {
                    venues.add(Venue.WEEKEND_TRIP);
                    if (formality >= 2.2) {
                        venues.add(Venue.BRUNCH);
                    }
                }
                case SPORT -> {
                    venues.add(Venue.GYM);
                    venues.add(Venue.WALK);
                }
            }
        }
        boolean travel = shoes != null && TRAVEL_SHOES.contains(shoes.subcategory())
                && formality >= 2 && formality <= 3.9 && subs.stream().noneMatch(DRESSY::contains);
        if (travel) {
            venues.add(Venue.TRAVEL);
        }
        return venues.stream().limit(5).toList();
    }
}
