package com.ryvenca.color;

import java.util.List;

/**
 * Result of dominant color detection.
 *
 * @param measured   the measured garment color (cluster centroid)
 * @param color      best matching standard class
 * @param confidence 0..1
 * @param candidates top classes (best first)
 * @param patternLikely true when the garment area has two strong, very different colors
 * @param focusX     horizontal center of the detected garment area (0..1)
 * @param focusY     vertical center of the detected garment area (0..1)
 */
public record ColorDetection(Lab measured, ColorName color, double confidence, List<ColorClassifier.Match> candidates,
                             boolean patternLikely, double focusX, double focusY) {

    public String measuredHex() {
        return measured.toHex();
    }
}
