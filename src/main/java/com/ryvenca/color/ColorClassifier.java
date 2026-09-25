package com.ryvenca.color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Maps a measured LAB color onto the standard {@link ColorName} classes. */
public final class ColorClassifier {

    public record Match(ColorName color, double distance, double score) {
    }

    private ColorClassifier() {
    }

    /** All classes ranked from best to worst, with a 0..1 closeness score. */
    public static List<Match> rank(Lab lab) {
        Map<ColorName, Double> best = new EnumMap<>(ColorName.class);
        for (ColorName color : ColorName.values()) {
            double min = Double.MAX_VALUE;
            for (Lab prototype : color.prototypes()) {
                min = Math.min(min, ColorScience.deltaE2000(lab, prototype));
            }
            best.put(color, adjust(color, lab, min));
        }
        List<Match> matches = new ArrayList<>();
        best.forEach((color, distance) -> matches.add(new Match(color, distance, closeness(distance))));
        matches.sort(Comparator.comparingDouble(Match::distance));
        return matches;
    }

    public static ColorName classify(Lab lab) {
        return rank(lab).getFirst().color();
    }

    private static double closeness(double distance) {
        return Math.max(0, Math.min(1, 1 - distance / 30.0));
    }

    /**
     * Guard rails for achromatic colors: CIEDE2000 alone can call a slightly warm off-white "cream"
     * or a dark desaturated navy "black". Very low chroma colors are pulled towards the gray axis.
     */
    private static double adjust(ColorName color, Lab lab, double distance) {
        double chroma = lab.chroma();
        boolean achromatic = color == ColorName.BLACK || color == ColorName.WHITE || color == ColorName.GRAY;
        if (chroma < 4 && !achromatic) {
            return distance + 6;
        }
        if (chroma > 18 && achromatic) {
            return distance + 6;
        }
        return distance;
    }
}
