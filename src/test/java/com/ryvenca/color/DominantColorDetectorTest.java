package com.ryvenca.color;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class DominantColorDetectorTest {

    private final DominantColorDetector detector = new DominantColorDetector();

    @ParameterizedTest(name = "{2} garment on {0}")
    @CsvSource({
            "#F2EEE8, #1F2A44, NAVY",        // navy shirt on a white bed sheet
            "#F2EEE8, #C8B596, BEIGE",       // beige blazer on a cream bed
            "#8A6A4F, #F4F3EF, WHITE",       // white shirt on a wooden floor
            "#8A6A4F, #1C1C1C, BLACK",       // black tee on a wooden floor
            "#D9D5CF, #4F6A8E, BLUE",        // jeans on a light gray sofa
            "#EDE7DD, #7B2433, BURGUNDY",    // burgundy knit on a cream wall
            "#3A3A3A, #E8B4C3, PINK",        // pink blouse on a dark chair
            "#F5F5F2, #6A6A3C, OLIVE",       // olive trousers on a white floor
            "#C9B8A3, #B8272E, RED",         // red top on a beige carpet
            "#EFEFEF, #5E3B24, BROWN"        // brown loafer on a white table
    })
    void detectsGarmentColorDespiteBackground(String background, String garment, ColorName expected) {
        BufferedImage photo = SyntheticPhotos.garmentOn(background, garment, 900, 1200, 7);
        ColorDetection detection = detector.detect(photo);
        assertThat(detection.color()).isEqualTo(expected);
        assertThat(detection.candidates()).hasSize(3);
        assertThat(detection.candidates().getFirst().color()).isEqualTo(expected);
        assertThat(detection.confidence()).isBetween(0.3, 1.0);
        assertThat(detection.focusX()).isBetween(0.3, 0.7);
    }

    @ParameterizedTest(name = "{2} shoes on planks {0}")
    @CsvSource({
            "#9A7656, #1C1C1C, BLACK",       // black loafers on a wooden floor
            "#9A7656, #F2F1EE, WHITE",       // white sneakers on a wooden floor
            "#3B3B3D, #F2F1EE, WHITE",       // white sneakers on a dark floor
            "#C9B79E, #5E3B24, BROWN",       // brown boots on light oak
            "#6B4A33, #1F2A44, NAVY"         // navy sneakers on dark walnut
    })
    void detectsSmallObjectsOnTexturedFloors(String planks, String shoes, ColorName expected) {
        for (long seed = 1; seed <= 4; seed++) {
            for (double y : new double[] {0.5, 0.62}) {
                ColorDetection detection = detector.detect(SyntheticPhotos.shoesOnPlanks(planks, shoes, 900, 1200, seed, y));
                assertThat(detection.color()).as("seed %d, y %.2f", seed, y).isEqualTo(expected);
            }
        }
    }

    @Test
    void whiteShirtOnAWhiteSheetIsWhiteNotItsShadow() {
        ColorDetection detection = detector.detect(SyntheticPhotos.garmentOn("#F4F1EC", "#F3F2EE", 900, 1200, 9));
        assertThat(detection.color()).isEqualTo(ColorName.WHITE);
    }

    @Test
    void closeUpPhotoIsNotMistakenForBackground() {
        ColorDetection detection = detector.detect(SyntheticPhotos.closeUp("#6A6A3C", 800, 1000, 3));
        assertThat(detection.color()).isEqualTo(ColorName.OLIVE);
    }

    @Test
    void measuredHexIsTheActualToneNotTheCanonicalSwatch() {
        ColorDetection detection = detector.detect(SyntheticPhotos.garmentOn("#F2EEE8", "#C19A6B", 600, 800, 11));
        assertThat(detection.color()).isEqualTo(ColorName.BEIGE);
        assertThat(ColorScience.deltaE2000(detection.measured(), Lab.ofHex("#C19A6B"))).isLessThan(6);
    }

    @Test
    void stripedGarmentIsFlaggedAsPatterned() {
        ColorDetection detection = detector.detect(
                SyntheticPhotos.garmentOn("#8A6A4F", "#EFE6D2", "#1C1C1C", 900, 1200, 5));
        assertThat(detection.patternLikely()).isTrue();
        assertThat(detection.color()).isIn(ColorName.CREAM, ColorName.BLACK, ColorName.WHITE);
    }

    @Test
    void plainGarmentIsNotFlaggedAsPatterned() {
        ColorDetection detection = detector.detect(SyntheticPhotos.garmentOn("#F2EEE8", "#1F2A44", 900, 1200, 5));
        assertThat(detection.patternLikely()).isFalse();
    }
}
