package com.ryvenca.outfit.engine;

import static com.ryvenca.outfit.engine.Wardrobes.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.StylePreference;
import com.ryvenca.color.PaletteLibrary;

class OutfitEngineTest {

    private static final PaletteLibrary PALETTES = new PaletteLibrary();
    private final OutfitEngine engine = new OutfitEngine(capsule(), PALETTES);
    private final OutfitRequest autumn = new OutfitRequest(Season.AUTUMN, null, Set.of(), 1);

    private int score(WardrobeItem... items) {
        return engine.evaluate(List.of(items), autumn).score();
    }

    @Test
    void classicNeutralOutfitScoresHigh() {
        assertThat(score(BEIGE_BLAZER, WHITE_SHIRT, NAVY_TROUSERS, BROWN_LOAFER)).isGreaterThanOrEqualTo(88);
        assertThat(score(BLACK_TEE, BLUE_JEANS, WHITE_SNEAKER)).isGreaterThanOrEqualTo(85);
    }

    @Test
    void clashingColorsScoreClearlyLower() {
        int good = score(RED_BLOUSE, BLACK_TROUSERS, BROWN_LOAFER);
        int clash = score(RED_BLOUSE, ORANGE_SKIRT, PURPLE_HEELS);
        assertThat(good - clash).isGreaterThanOrEqualTo(20);
        assertThat(clash).isLessThan(70);
    }

    @Test
    void formalityMismatchIsPenalized() {
        assertThat(score(BLACK_TEE, SWEATPANTS, RUNNING)).isGreaterThan(score(BLACK_TEE, SWEATPANTS, BROWN_LOAFER) + 15);
        assertThat(score(BLACK_DRESS, PURPLE_HEELS)).isGreaterThan(score(BLACK_DRESS, RUNNING) + 20);
    }

    @Test
    void seasonMattersForHeavyLayers() {
        OutfitRequest summer = new OutfitRequest(Season.SUMMER, null, Set.of(), 1);
        OutfitRequest winter = new OutfitRequest(Season.WINTER, null, Set.of(), 1);
        List<WardrobeItem> withPuffer = List.of(PUFFER, BLACK_TEE, BLUE_JEANS, WHITE_SNEAKER);
        assertThat(engine.evaluate(withPuffer, winter).score()).isGreaterThan(engine.evaluate(withPuffer, summer).score());
    }

    @Test
    void scoreIsTheWeightedCombinationOfTheFiveDimensions() {
        OutfitEvaluation e = engine.evaluate(List.of(BEIGE_BLAZER, WHITE_SHIRT, NAVY_TROUSERS, BROWN_LOAFER), autumn);
        double raw = 0.40 * e.color().score() + 0.25 * e.compatibility().score() + 0.15 * e.season().score()
                + 0.15 * e.occasion().score() + 0.05 * e.style().score();
        assertThat(e.raw()).isEqualTo(raw);
        assertThat(e.score()).isBetween(0, 100);
    }

    @Test
    void suggestionsAreValidDistinctAndDiverse() {
        List<OutfitEvaluation> outfits = engine.suggest(autumn, 8);
        assertThat(outfits).hasSize(8);
        Set<String> keys = new HashSet<>();
        for (OutfitEvaluation e : outfits) {
            assertThat(OutfitScorer.structureProblem(e.items())).isNull();
            assertThat(keys.add(e.key())).isTrue();
            assertThat(e.score()).isGreaterThanOrEqualTo(70);
        }
        // Diversity: no top/bottom/dress base is repeated in the first four suggestions.
        Set<List<Long>> bases = new HashSet<>();
        for (OutfitEvaluation e : outfits.subList(0, 4)) {
            bases.add(e.items().stream().filter(i -> i.role() == OutfitRole.TOP || i.role() == OutfitRole.BOTTOM
                    || i.role() == OutfitRole.DRESS).map(WardrobeItem::id).sorted().toList());
        }
        assertThat(bases).hasSize(4);
    }

    @Test
    void differentSeedsGiveFreshButStillGoodSuggestions() {
        List<String> first = engine.suggest(new OutfitRequest(Season.AUTUMN, null, Set.of(), 1), 5).stream()
                .map(OutfitEvaluation::key).toList();
        List<String> again = engine.suggest(new OutfitRequest(Season.AUTUMN, null, Set.of(), 1), 5).stream()
                .map(OutfitEvaluation::key).toList();
        List<String> other = engine.suggest(new OutfitRequest(Season.AUTUMN, null, Set.of(), 987654), 5).stream()
                .map(OutfitEvaluation::key).toList();
        assertThat(again).isEqualTo(first);
        assertThat(other).isNotEqualTo(first);
    }

    @Test
    void summerSuggestionsNeverContainHeavyLayers() {
        OutfitRequest summer = new OutfitRequest(Season.SUMMER, null, Set.of(), 3);
        for (OutfitEvaluation e : engine.suggest(summer, 10)) {
            assertThat(e.items()).doesNotContain(PUFFER, CREAM_SWEATER);
        }
    }

    @Test
    void occasionFilterFavorsMatchingOutfits() {
        OutfitRequest office = new OutfitRequest(Season.AUTUMN, Occasion.OFFICE, Set.of(), 1);
        List<OutfitEvaluation> outfits = engine.suggest(office, 5);
        assertThat(outfits).isNotEmpty();
        for (OutfitEvaluation e : outfits) {
            assertThat(e.items()).doesNotContain(SWEATPANTS, RUNNING);
            assertThat(e.occasion().score()).isGreaterThan(0.6);
        }
    }

    @Test
    void stylePreferenceShiftsTheScore() {
        List<WardrobeItem> sporty = List.of(BLACK_TEE, SWEATPANTS, RUNNING);
        int forSportFan = engine.evaluate(sporty, new OutfitRequest(Season.AUTUMN, null, Set.of(StylePreference.SPORT), 1)).score();
        int forClassicFan = engine.evaluate(sporty, new OutfitRequest(Season.AUTUMN, null, Set.of(StylePreference.BUSINESS), 1)).score();
        assertThat(forSportFan).isGreaterThan(forClassicFan);
    }

    @Test
    void pairingsForABlazerOfferAlternatives() {
        Pairings pairings = engine.pairings(BEIGE_BLAZER, autumn, 6, 6);
        assertThat(pairings.outfits()).isNotEmpty();
        pairings.outfits().forEach(e -> assertThat(e.items()).contains(BEIGE_BLAZER));
        List<OutfitRole> roles = pairings.matches().stream().map(Pairings.RoleMatches::role).toList();
        assertThat(roles).startsWith(OutfitRole.TOP, OutfitRole.BOTTOM, OutfitRole.SHOES).doesNotContain(OutfitRole.OUTERWEAR);
        Pairings.RoleMatches bottoms = pairings.matches().stream().filter(m -> m.role() == OutfitRole.BOTTOM).findFirst().orElseThrow();
        assertThat(bottoms.items()).extracting(Pairings.Match::item).contains(NAVY_TROUSERS, BLUE_JEANS);
        assertThat(bottoms.items().getFirst().score()).isGreaterThanOrEqualTo(bottoms.items().getLast().score());
    }

    @Test
    void pairingsForATopExcludeDresses() {
        Pairings pairings = engine.pairings(WHITE_SHIRT, autumn, 6, 6);
        assertThat(pairings.matches()).extracting(Pairings.RoleMatches::role).doesNotContain(OutfitRole.DRESS, OutfitRole.TOP);
        pairings.outfits().forEach(e -> assertThat(OutfitScorer.structureProblem(e.items())).isNull());
    }

    @Test
    void pairingsForABagAddTheBagToEveryOutfit() {
        Pairings pairings = engine.pairings(BROWN_BAG, autumn, 6, 4);
        assertThat(pairings.outfits()).isNotEmpty().allSatisfy(e -> assertThat(e.items()).contains(BROWN_BAG));
    }

    @Test
    void similarOutfitsExcludeTheReference() {
        OutfitEvaluation reference = engine.evaluate(List.of(BEIGE_BLAZER, WHITE_SHIRT, NAVY_TROUSERS, BROWN_LOAFER), autumn);
        List<OutfitEvaluation> similar = engine.similar(reference, autumn, 6);
        assertThat(similar).isNotEmpty().hasSizeLessThanOrEqualTo(6);
        assertThat(similar).extracting(OutfitEvaluation::key).doesNotContain(reference.key());
    }

    @Test
    void countsReadyOutfits() {
        int ready = engine.countReady(autumn, 70, 99);
        assertThat(ready).isBetween(10, 99);
        assertThat(engine.countReady(autumn, 70, 5)).isEqualTo(5);
    }

    @Test
    void structureValidation() {
        assertThat(OutfitScorer.structureProblem(List.of(WHITE_SHIRT, NAVY_TROUSERS))).isNotNull();
        assertThat(OutfitScorer.structureProblem(List.of(BLACK_DRESS, WHITE_SHIRT, PURPLE_HEELS))).isNotNull();
        assertThat(OutfitScorer.structureProblem(List.of(BLACK_DRESS, PURPLE_HEELS, BROWN_BAG))).isNull();
    }

    @Test
    void emptyWardrobeYieldsNothing() {
        OutfitEngine empty = new OutfitEngine(List.of(), PALETTES);
        assertThat(empty.suggest(autumn, 5)).isEmpty();
        assertThat(empty.countReady(autumn, 70, 99)).isZero();
    }
}
