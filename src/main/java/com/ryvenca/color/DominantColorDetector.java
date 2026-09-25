package com.ryvenca.color;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

/**
 * Classic (non-generative) dominant garment color detection for ordinary phone photos.
 *
 * <p>Pipeline: downscale → RGB→LAB → estimate the background from the image border (k-means) →
 * keep pixels of a central region of interest that differ from the background (falling back to the
 * whole region for close-ups where the garment fills the frame) → center-weighted
 * k-means on those pixels → merge near clusters → dominant cluster → map to the standard palette
 * with CIEDE2000.
 */
@Component
public class DominantColorDetector {

    private static final int ANALYSIS_SIZE = 160;
    private static final double BORDER = 0.07;
    private static final double BACKGROUND_DISTANCE_SQ = 13 * 13;
    private static final int GARMENT_CLUSTERS = 5;
    private static final double MIN_FOREGROUND = 0.04;

    public ColorDetection detect(BufferedImage source) {
        BufferedImage img = ImageScaling.fitWithin(source, ANALYSIS_SIZE);
        int w = img.getWidth();
        int h = img.getHeight();
        int n = w * h;
        double[] l = new double[n];
        double[] a = new double[n];
        double[] b = new double[n];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Lab lab = ColorScience.rgbToLab(img.getRGB(x, y));
                int i = y * w + x;
                l[i] = lab.l();
                a[i] = lab.a();
                b[i] = lab.b();
            }
        }

        // 1. Background model from the border strip.
        int bw = Math.max(1, (int) Math.round(w * BORDER));
        int bh = Math.max(1, (int) Math.round(h * BORDER));
        List<Integer> border = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x < bw || x >= w - bw || y < bh || y >= h - bh) {
                    border.add(y * w + x);
                }
            }
        }
        // Textured backgrounds (wood planks, folds) have several tones, hence several clusters.
        Clusters borderClusters = kmeans(toArray(border), l, a, b, null, 4, 11);
        List<double[]> background = new ArrayList<>();
        for (int k = 0; k < borderClusters.size(); k++) {
            if (borderClusters.weight[k] >= 0.1) {
                background.add(borderClusters.center(k));
            }
        }

        // 2. Foreground pixels inside the region of interest (clustered with center weighting).
        List<Integer> roi = new ArrayList<>();
        List<Integer> foreground = new ArrayList<>();
        for (int y = (int) (h * 0.06); y < h * 0.94; y++) {
            for (int x = (int) (w * 0.06); x < w * 0.94; x++) {
                int i = y * w + x;
                roi.add(i);
                if (!isBackground(i, l, a, b, background)) {
                    foreground.add(i);
                }
            }
        }
        // Close-up: the garment touches the border and was removed with the background, leaving only
        // specks behind. Then the whole region of interest is the garment.
        boolean closeUp = foreground.size() < roi.size() * MIN_FOREGROUND;
        Analysis analysis = analyze(closeUp ? roi : foreground, w, h, l, a, b);
        if (!closeUp && isShadowOfBackground(analysis.measured(), background, w, h, l, a, b)) {
            // White shirt on a white sheet: the garment matched the background and only its shading
            // survived. The garment is the background-colored area itself.
            analysis = analyze(roi, w, h, l, a, b);
        }
        return analysis.toDetection();
    }

    private record Analysis(Lab measured, Clusters clusters, int dominant, double focusX, double focusY) {

        ColorDetection toDetection() {
            List<ColorClassifier.Match> ranked = ColorClassifier.rank(measured);
            ColorClassifier.Match best = ranked.getFirst();
            double margin = ranked.get(1).distance() - best.distance();
            double dominance = clusters.weight[dominant];
            double confidence = clamp(0.45 * best.score() + 0.35 * Math.min(1, margin / 8.0)
                    + 0.2 * Math.min(1, dominance / 0.6));
            // Pattern: a large share of the garment is a clearly different color (shading of that second
            // color may be split over several clusters, so their weights are summed).
            double contrasting = 0;
            for (int k = 0; k < clusters.size(); k++) {
                if (k != dominant && clusters.weight[k] > 0.04 && ColorScience.deltaE2000(measured,
                        new Lab(clusters.l[k], clusters.a[k], clusters.b[k])) > 25) {
                    contrasting += clusters.weight[k];
                }
            }
            return new ColorDetection(measured, best.color(), confidence, ranked.subList(0, 3), contrasting > 0.3,
                    focusX, focusY);
        }
    }

    private static Analysis analyze(List<Integer> pixels, int w, int h, double[] l, double[] a, double[] b) {
        double cx = (w - 1) / 2.0;
        double cy = (h - 1) / 2.0;
        double sx = w * 0.32;
        double sy = h * 0.34;
        int[] idx = toArray(pixels);
        double[] weights = new double[idx.length];
        double fx = 0;
        double fy = 0;
        double wsum = 0;
        for (int j = 0; j < idx.length; j++) {
            int x = idx[j] % w;
            int y = idx[j] / w;
            double dx = (x - cx) / sx;
            double dy = (y - cy) / sy;
            weights[j] = Math.exp(-0.5 * (dx * dx + dy * dy));
            fx += x * weights[j];
            fy += y * weights[j];
            wsum += weights[j];
        }
        Clusters clusters = kmeans(idx, l, a, b, weights, GARMENT_CLUSTERS, 12).merged(10);
        int dominant = clusters.dominant();
        Lab measured = new Lab(clusters.l[dominant], clusters.a[dominant], clusters.b[dominant]);
        double focusX = wsum > 0 ? fx / wsum / Math.max(1, w - 1) : 0.5;
        double focusY = wsum > 0 ? fy / wsum / Math.max(1, h - 1) : 0.5;
        return new Analysis(measured, clusters, dominant, clamp(focusX), clamp(focusY));
    }

    /**
     * True when the measured color is merely a darker shade (same hue and saturation) of a background
     * color that also covers much of the image center.
     */
    private static boolean isShadowOfBackground(Lab measured, List<double[]> background, int w, int h, double[] l,
                                                double[] a, double[] b) {
        List<Integer> core = new ArrayList<>();
        for (int y = (int) (h * 0.3); y < h * 0.7; y++) {
            for (int x = (int) (w * 0.3); x < w * 0.7; x++) {
                core.add(y * w + x);
            }
        }
        for (double[] c : background) {
            Lab bg = new Lab(c[0], c[1], c[2]);
            boolean darker = measured.l() < bg.l() - 6;
            boolean sameSaturation = Math.abs(measured.chroma() - bg.chroma()) < 6;
            boolean sameHue = (measured.chroma() < 10 && bg.chroma() < 10)
                    || ColorScience.hueDistance(measured.hue(), bg.hue()) < 20;
            if (darker && sameSaturation && sameHue && share(core, l, a, b, c) > 0.35) {
                return true;
            }
        }
        return false;
    }

    private static double share(List<Integer> pixels, double[] l, double[] a, double[] b, double[] center) {
        int close = 0;
        for (int i : pixels) {
            if (ColorScience.distanceSq(l[i], a[i], b[i], center[0], center[1], center[2]) < BACKGROUND_DISTANCE_SQ) {
                close++;
            }
        }
        return pixels.isEmpty() ? 0 : close / (double) pixels.size();
    }

    private static boolean isBackground(int i, double[] l, double[] a, double[] b, List<double[]> background) {
        for (double[] c : background) {
            if (ColorScience.distanceSq(l[i], a[i], b[i], c[0], c[1], c[2]) < BACKGROUND_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    private static int[] toArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /** Weighted k-means with deterministic k-means++ seeding. */
    static Clusters kmeans(int[] idx, double[] l, double[] a, double[] b, double[] weights, int k, int iterations) {
        int n = idx.length;
        k = Math.max(1, Math.min(k, n));
        Random random = new Random(42);
        double[] cl = new double[k];
        double[] ca = new double[k];
        double[] cb = new double[k];
        int first = idx[random.nextInt(n)];
        cl[0] = l[first];
        ca[0] = a[first];
        cb[0] = b[first];
        double[] dist = new double[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        for (int c = 1; c < k; c++) {
            double total = 0;
            for (int j = 0; j < n; j++) {
                int i = idx[j];
                dist[j] = Math.min(dist[j], ColorScience.distanceSq(l[i], a[i], b[i], cl[c - 1], ca[c - 1], cb[c - 1]));
                total += dist[j] * w(weights, j);
            }
            double target = random.nextDouble() * total;
            int chosen = n - 1;
            double acc = 0;
            for (int j = 0; j < n; j++) {
                acc += dist[j] * w(weights, j);
                if (acc >= target) {
                    chosen = j;
                    break;
                }
            }
            cl[c] = l[idx[chosen]];
            ca[c] = a[idx[chosen]];
            cb[c] = b[idx[chosen]];
        }

        int[] assignment = new int[n];
        double[] mass = new double[k];
        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 0; j < n; j++) {
                int i = idx[j];
                double best = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    double d = ColorScience.distanceSq(l[i], a[i], b[i], cl[c], ca[c], cb[c]);
                    if (d < best) {
                        best = d;
                        assignment[j] = c;
                    }
                }
            }
            double[] sl = new double[k];
            double[] sa = new double[k];
            double[] sb = new double[k];
            Arrays.fill(mass, 0);
            for (int j = 0; j < n; j++) {
                int i = idx[j];
                int c = assignment[j];
                double wj = w(weights, j);
                sl[c] += l[i] * wj;
                sa[c] += a[i] * wj;
                sb[c] += b[i] * wj;
                mass[c] += wj;
            }
            for (int c = 0; c < k; c++) {
                if (mass[c] > 0) {
                    cl[c] = sl[c] / mass[c];
                    ca[c] = sa[c] / mass[c];
                    cb[c] = sb[c] / mass[c];
                }
            }
        }
        double totalMass = Arrays.stream(mass).sum();
        double[] share = new double[k];
        for (int c = 0; c < k; c++) {
            share[c] = totalMass > 0 ? mass[c] / totalMass : 0;
        }
        return new Clusters(cl, ca, cb, share);
    }

    private static double w(double[] weights, int j) {
        return weights == null ? 1 : weights[j];
    }

    record Clusters(double[] l, double[] a, double[] b, double[] weight) {

        int size() {
            return weight.length;
        }

        double[] center(int k) {
            return new double[] {l[k], a[k], b[k]};
        }

        int dominant() {
            int best = 0;
            for (int k = 1; k < weight.length; k++) {
                if (weight[k] > weight[best]) {
                    best = k;
                }
            }
            return best;
        }

        /** Merges clusters whose centers are closer than {@code maxDeltaE}; shadows and highlights of one fabric. */
        Clusters merged(double maxDeltaE) {
            int k = weight.length;
            double[] ml = l.clone();
            double[] ma = a.clone();
            double[] mb = b.clone();
            double[] mw = weight.clone();
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = 0; i < k && !changed; i++) {
                    for (int j = i + 1; j < k && !changed; j++) {
                        if (mw[i] == 0 || mw[j] == 0) {
                            continue;
                        }
                        double de = ColorScience.deltaE2000(new Lab(ml[i], ma[i], mb[i]), new Lab(ml[j], ma[j], mb[j]));
                        if (de < maxDeltaE) {
                            double total = mw[i] + mw[j];
                            ml[i] = (ml[i] * mw[i] + ml[j] * mw[j]) / total;
                            ma[i] = (ma[i] * mw[i] + ma[j] * mw[j]) / total;
                            mb[i] = (mb[i] * mw[i] + mb[j] * mw[j]) / total;
                            mw[i] = total;
                            mw[j] = 0;
                            changed = true;
                        }
                    }
                }
            }
            return new Clusters(ml, ma, mb, mw);
        }
    }
}
