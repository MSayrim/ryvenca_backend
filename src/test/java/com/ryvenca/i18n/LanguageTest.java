package com.ryvenca.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LanguageTest {

    @Test
    void supportsTurkishPlusTheFifteenMostSpokenLanguages() {
        assertThat(Language.codes()).containsExactly(
                "tr", "en", "zh", "hi", "es", "ar", "fr", "bn", "pt", "ru", "id", "ur", "de", "ja", "vi", "ko");
        assertThat(List.of(Language.values()).stream().filter(Language::rtl)).containsExactly(Language.AR, Language.UR);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "zh-CN,zh;q=0.9,en;q=0.8 | zh",
            "pt-BR                   | pt",
            "fr-CH, fr;q=0.9         | fr",
            "sw, ar;q=0.5            | ar",
            "sw                      | en",
            "in-ID                   | id",
            "*                       | en",
            "ja_JP                   | ja"
    })
    void resolvesAcceptLanguage(String header, String expected) {
        assertThat(LanguageLocaleResolver.resolve(header).code()).isEqualTo(expected);
    }

    @Test
    void missingHeaderMeansTurkish() {
        assertThat(LanguageLocaleResolver.resolve(null)).isEqualTo(Language.TR);
        assertThat(LanguageLocaleResolver.resolve(" ")).isEqualTo(Language.TR);
    }

    @Test
    void localizerJoinsListsAndPhrases() {
        Localizer tr = TestTexts.localizer(Language.TR);
        Localizer en = TestTexts.localizer(Language.EN);
        assertThat(tr.join(List.of("bej", "beyaz", "lacivert"))).isEqualTo("bej, beyaz ve lacivert");
        assertThat(en.join(List.of("beige", "white"))).isEqualTo("beige and white");
        assertThat(en.join(List.of("beige"))).isEqualTo("beige");
        assertThat(tr.phrase(com.ryvenca.color.ColorName.BEIGE, com.ryvenca.catalog.Subcategory.BLAZER)).isEqualTo("bej blazer");
        assertThat(en.phrase(com.ryvenca.color.ColorName.LIGHT_BLUE, com.ryvenca.catalog.Subcategory.SHIRT)).isEqualTo("light blue shirt");
        assertThat(tr.capitalize("ışık")).isEqualTo("Işık");
    }

    @Test
    void placeholdersAreReplacedWithoutMessageFormatQuirks() {
        assertThat(Localizer.format("l'{0} et {1} {2}", "a", "b")).isEqualTo("l'a et b {2}");
        assertThat(Localizer.format("{x} {0}", "y")).isEqualTo("{x} y");
    }
}
