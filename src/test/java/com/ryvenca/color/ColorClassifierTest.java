package com.ryvenca.color;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ColorClassifierTest {

    @ParameterizedTest
    @EnumSource(ColorName.class)
    void canonicalSwatchMapsToItsOwnClass(ColorName color) {
        assertThat(ColorClassifier.classify(color.lab())).isEqualTo(color);
    }

    /** Real-world garment tones measured from ordinary photos. */
    @ParameterizedTest
    @CsvSource({
            "#4F6A8E, BLUE",        // mid-wash denim
            "#6F87A6, BLUE",        // light washed denim in shade
            "#C19A6B, BEIGE",       // camel coat
            "#3F4245, GRAY",        // charcoal
            "#F4F3EF, WHITE",       // white shirt in daylight
            "#E9DFC9, CREAM",       // ecru knit
            "#5E3B24, BROWN",       // dark brown leather
            "#212A40, NAVY",        // navy trousers
            "#181818, BLACK",
            "#6A6A3C, OLIVE",       // olive chinos
            "#7B2433, BURGUNDY",
            "#E8B4C3, PINK",
            "#B5562A, ORANGE",      // rust
            "#D8B53A, YELLOW",      // mustard
            "#9DB8D6, LIGHT_BLUE",
            "#2F6B45, GREEN",
            "#6B4B8C, PURPLE",
            "#C62B30, RED"
    })
    void realWorldTonesAreClassifiedSensibly(String hex, ColorName expected) {
        assertThat(ColorClassifier.classify(Lab.ofHex(hex))).isEqualTo(expected);
    }

    @Test
    void rankingIsBestFirstWithScores() {
        var ranked = ColorClassifier.rank(Lab.ofHex("#CDB89A"));
        assertThat(ranked).hasSize(ColorName.values().length);
        assertThat(ranked.getFirst().color()).isEqualTo(ColorName.BEIGE);
        assertThat(ranked.get(0).distance()).isLessThanOrEqualTo(ranked.get(1).distance());
        assertThat(ranked.getFirst().score()).isBetween(0.9, 1.0);
    }
}
