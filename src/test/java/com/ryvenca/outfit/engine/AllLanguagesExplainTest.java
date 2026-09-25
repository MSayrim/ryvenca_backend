package com.ryvenca.outfit.engine;

import static com.ryvenca.outfit.engine.Wardrobes.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.color.PaletteLibrary;
import com.ryvenca.i18n.Language;
import com.ryvenca.i18n.TestTexts;

/** Renders explanations for many outfits in every language: no raw keys, no unfilled placeholders. */
class AllLanguagesExplainTest {

    private static final OutfitEngine ENGINE = new OutfitEngine(capsule(), new PaletteLibrary());

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyLanguageRendersCompleteExplanations(Language language) {
        OutfitExplainer explainer = new OutfitExplainer(TestTexts.localizer(language));
        List<OutfitEvaluation> outfits = new ArrayList<>();
        for (Season season : Season.values()) {
            outfits.addAll(ENGINE.suggest(new OutfitRequest(season, null, Set.of(), 7), 8));
            outfits.addAll(ENGINE.suggest(new OutfitRequest(season, Occasion.OFFICE, Set.of(), 3), 4));
        }
        outfits.add(ENGINE.evaluate(List.of(RED_BLOUSE, ORANGE_SKIRT, PURPLE_HEELS),
                new OutfitRequest(Season.WINTER, Occasion.SPORT, Set.of(), 1)));
        outfits.add(ENGINE.evaluate(List.of(BLACK_TEE, SWEATPANTS, BROWN_LOAFER),
                new OutfitRequest(Season.SUMMER, Occasion.OFFICE, Set.of(), 1)));
        for (OutfitEvaluation e : outfits) {
            OutfitStory story = explainer.explain(e);
            List<String> texts = new ArrayList<>(List.of(story.title(), story.description()));
            story.reasons().forEach(r -> {
                texts.add(r.title());
                texts.add(r.text());
            });
            for (String text : texts) {
                assertThat(text).as("%s: %s", language, text).isNotBlank()
                        .doesNotContainPattern("\\{\\d+}")
                        .doesNotContainPattern("\\b(explain|title|grammar|reason|color|subcategory|venue|texture|role)\\.[A-Za-z_]");
            }
        }
    }
}
