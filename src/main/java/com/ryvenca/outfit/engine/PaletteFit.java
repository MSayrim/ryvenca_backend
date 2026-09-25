package com.ryvenca.outfit.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ryvenca.color.ColorPalette;
import com.ryvenca.color.ColorScience;
import com.ryvenca.color.Lab;
import com.ryvenca.color.PaletteLibrary;

/**
 * Precomputed CIEDE2000 distances between every wardrobe item and every palette swatch, so outfit
 * scoring does not repeat expensive color math for each candidate combination.
 */
final class PaletteFit {

    /** Distance at or below which a color counts as the swatch itself. */
    static final double EXACT = 6;
    /** Distance at or above which a color no longer belongs to the swatch. */
    static final double FAR = 22;
    /** Distance under which a swatch counts as "used" by an outfit. */
    static final double USED = 12;

    private final PaletteLibrary library;
    private final Map<Long, double[]> fitByItem = new HashMap<>();
    private final Map<Long, int[]> swatchByItem = new HashMap<>();

    PaletteFit(PaletteLibrary library, List<WardrobeItem> items) {
        this.library = library;
        for (WardrobeItem item : items) {
            add(item);
        }
    }

    PaletteLibrary library() {
        return library;
    }

    void add(WardrobeItem item) {
        int n = library.size();
        double[] fit = new double[n];
        int[] swatch = new int[n];
        for (int p = 0; p < n; p++) {
            ColorPalette palette = library.get(p);
            double best = Double.MAX_VALUE;
            int bestIndex = -1;
            for (int s = 0; s < palette.swatches().size(); s++) {
                Lab lab = palette.swatches().get(s);
                double d = ColorScience.deltaE2000(item.lab(), lab);
                if (d < best) {
                    best = d;
                    bestIndex = s;
                }
            }
            fit[p] = Math.max(0, Math.min(1, (FAR - best) / (FAR - EXACT)));
            swatch[p] = best <= USED ? bestIndex : -1;
        }
        fitByItem.put(item.id(), fit);
        swatchByItem.put(item.id(), swatch);
    }

    double fit(WardrobeItem item, int palette) {
        return fitByItem.get(item.id())[palette];
    }

    /** Index of the palette swatch the item uses, or -1. */
    int swatch(WardrobeItem item, int palette) {
        return swatchByItem.get(item.id())[palette];
    }
}
