package com.ryvenca.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ryvenca.color.SyntheticPhotos;
import com.ryvenca.common.ApiException;

class ImageProcessorTest {

    private static final int RED = 0xFF0000;
    private static final int GREEN = 0x00FF00;

    private final ImageProcessor processor = new ImageProcessor();

    /**
     * A 4x2 image with a red pixel at (0,0) and a green one at (1,0) must end up where the EXIF
     * orientation says the top-left corner is displayed.
     */
    @ParameterizedTest(name = "orientation {0}")
    @CsvSource({
            "1, 4, 2, 0, 0, 1, 0",
            "2, 4, 2, 3, 0, 2, 0",
            "3, 4, 2, 3, 1, 2, 1",
            "4, 4, 2, 0, 1, 1, 1",
            "5, 2, 4, 0, 0, 0, 1",
            "6, 2, 4, 1, 0, 1, 1",
            "7, 2, 4, 1, 3, 1, 2",
            "8, 2, 4, 0, 3, 0, 2"
    })
    void appliesExifOrientation(int orientation, int w, int h, int redX, int redY, int greenX, int greenY) {
        BufferedImage img = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, RED);
        img.setRGB(1, 0, GREEN);
        BufferedImage out = ImageProcessor.applyOrientation(img, orientation);
        assertThat(out.getWidth()).isEqualTo(w);
        assertThat(out.getHeight()).isEqualTo(h);
        assertThat(out.getRGB(redX, redY) & 0xFFFFFF).isEqualTo(RED);
        assertThat(out.getRGB(greenX, greenY) & 0xFFFFFF).isEqualTo(GREEN);
    }

    @Test
    void thumbnailIsFourByFiveCover() {
        BufferedImage photo = SyntheticPhotos.garmentOn("#F2EEE8", "#1F2A44", 1600, 900, 1);
        BufferedImage thumb = processor.thumbnail(photo, 0.5, 0.5);
        assertThat(thumb.getWidth()).isEqualTo(ImageProcessor.THUMB_WIDTH);
        assertThat(thumb.getHeight()).isEqualTo(ImageProcessor.THUMB_HEIGHT);
    }

    @Test
    void displayVersionIsDownscaledButNeverUpscaled() {
        BufferedImage big = SyntheticPhotos.garmentOn("#F2EEE8", "#1F2A44", 3000, 4000, 1);
        BufferedImage small = SyntheticPhotos.garmentOn("#F2EEE8", "#1F2A44", 600, 800, 1);
        assertThat(processor.display(big).getHeight()).isEqualTo(ImageProcessor.DISPLAY_MAX_EDGE);
        assertThat(processor.display(small).getHeight()).isEqualTo(800);
    }

    @Test
    void normalizationStretchesAFlatDarkPhotoGently() {
        BufferedImage flat = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int v = 40 + (x + y) / 4;
                flat.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        BufferedImage out = ImageProcessor.normalize(flat);
        int before = flat.getRGB(99, 99) & 0xFF;
        int after = out.getRGB(99, 99) & 0xFF;
        assertThat(after).isGreaterThan(before);
        assertThat(after).isLessThan(255);
    }

    @Test
    void jpegRoundTrip() {
        BufferedImage photo = SyntheticPhotos.garmentOn("#F2EEE8", "#1F2A44", 400, 500, 1);
        byte[] jpeg = processor.toJpeg(photo, 0.85f);
        BufferedImage decoded = processor.decode(jpeg);
        assertThat(decoded.getWidth()).isEqualTo(400);
        assertThat(decoded.getHeight()).isEqualTo(500);
    }

    @Test
    void rejectsNonImages() {
        assertThatThrownBy(() -> processor.decode("not an image".getBytes()))
                .isInstanceOf(ApiException.class);
    }
}
