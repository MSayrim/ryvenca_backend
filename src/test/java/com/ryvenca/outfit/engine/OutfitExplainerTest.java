package com.ryvenca.outfit.engine;

import static com.ryvenca.outfit.engine.Wardrobes.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.PaletteLibrary;
import com.ryvenca.i18n.Language;
import com.ryvenca.i18n.TestTexts;

class OutfitExplainerTest {

    private static final PaletteLibrary PALETTES = new PaletteLibrary();
    private final OutfitEngine engine = new OutfitEngine(capsule(), PALETTES);
    private final OutfitExplainer explainer = new OutfitExplainer(TestTexts.localizer(Language.TR));
    private final OutfitRequest autumn = new OutfitRequest(Season.AUTUMN, null, Set.of(), 1);

    private OutfitStory story(OutfitRequest request, WardrobeItem... items) {
        return explainer.explain(engine.evaluate(List.of(items), request));
    }

    @Test
    void explainsColorTextureAndOccasion() {
        OutfitStory story = story(autumn, BEIGE_BLAZER, WHITE_SHIRT, NAVY_TROUSERS, BROWN_LOAFER);
        assertThat(story.title()).isNotBlank();
        assertThat(story.description()).isNotBlank();
        assertThat(story.reasons()).extracting(OutfitStory.Reason::code).startsWith("COLOR", "TEXTURE", "OCCASION");
        assertThat(story.reasons()).extracting(OutfitStory.Reason::title)
                .contains("Renk Dengesi", "Doku Uyumu", "Kullanım Alanı");
        String color = story.reasons().getFirst().text();
        assertThat(color).containsIgnoringCase("bej").contains("lacivert");
        assertThat(story.reasons().get(2).text()).contains("ofis");
    }

    @Test
    void mentionsTheSmartCasualMix() {
        OutfitStory story = story(autumn, BEIGE_BLAZER, BLACK_TEE, BLUE_JEANS, WHITE_SNEAKER);
        assertThat(story.reasons().get(1).text()).contains("Bej blazer ile mavi jean");
    }

    @Test
    void singleAccentIsDescribedAsAccent() {
        OutfitStory story = story(autumn, RED_BLOUSE, BLACK_TROUSERS, BROWN_LOAFER);
        assertThat(story.reasons().getFirst().text()).contains("kırmızı ise kombine kontrollü bir canlılık katıyor");
    }

    @Test
    void warnsAboutClashesAndMismatches() {
        assertThat(story(autumn, RED_BLOUSE, ORANGE_SKIRT, PURPLE_HEELS).reasons().getFirst().text())
                .contains("nötr bir tonla");
        assertThat(story(autumn, BLACK_TEE, SWEATPANTS, BROWN_LOAFER).reasons().get(1).text())
                .contains("resmiyet farkı").contains("ayakkabı");
    }

    @Test
    void addsASeasonReasonForLayers() {
        OutfitStory story = story(autumn, TRENCH, WHITE_SHIRT, BLACK_TROUSERS, BROWN_LOAFER);
        assertThat(story.reasons()).extracting(OutfitStory.Reason::code).contains("SEASON");
    }

    @Test
    void flagsOccasionMismatchForRequestedOccasion() {
        OutfitRequest office = new OutfitRequest(Season.AUTUMN, Occasion.OFFICE, Set.of(), 1);
        OutfitStory story = story(office, BLACK_TEE, SWEATPANTS, RUNNING);
        assertThat(story.reasons().get(2).text()).startsWith("Ofis için biraz rahat kalabilir.");
    }

    @Test
    void namesAPaletteOnlyWhenItsSignatureColorIsWorn() {
        OutfitStory purple = story(autumn, BLACK_DRESS, PURPLE_HEELS);
        assertThat(purple.paletteName()).isNotNull();
        OutfitStory basic = story(autumn, BLACK_TEE, BLUE_JEANS, WHITE_SNEAKER);
        if (basic.paletteName() != null) {
            assertThat(basic.paletteName()).doesNotContain("Turuncu");
        }
    }

    @Test
    void avoidsTitlesAlreadyUsedInTheSameList() {
        OutfitEvaluation e = engine.evaluate(List.of(BEIGE_BLAZER, WHITE_SHIRT, NAVY_TROUSERS, BROWN_LOAFER), autumn);
        String preferred = explainer.explain(e).title();
        String alternative = explainer.explain(e, Set.of(preferred)).title();
        assertThat(alternative).isNotEqualTo(preferred).isNotBlank();
    }

    @Test
    void explainsInEnglish() {
        OutfitExplainer english = new OutfitExplainer(TestTexts.localizer(Language.EN));
        OutfitStory story = english.explain(engine.evaluate(List.of(BEIGE_BLAZER, BLACK_TEE, BLUE_JEANS, WHITE_SNEAKER), autumn));
        assertThat(story.reasons()).extracting(OutfitStory.Reason::title)
                .contains("Color Balance", "Texture Harmony", "Where to Wear");
        assertThat(story.reasons().get(1).text()).startsWith("The beige blazer with the blue jeans");
        assertThat(story.title()).doesNotContainPattern("[çğıöşüİ]");
    }
}
