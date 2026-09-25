package com.ryvenca.color;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Generates imperfect "phone photos" of a garment: textured background, shading, noise. */
public final class SyntheticPhotos {

    private SyntheticPhotos() {
    }

    public static BufferedImage garmentOn(String backgroundHex, String garmentHex, int width, int height, long seed) {
        return garmentOn(backgroundHex, garmentHex, null, width, height, seed);
    }

    /** @param stripeHex when not null the garment gets horizontal stripes of this color. */
    public static BufferedImage garmentOn(String backgroundHex, String garmentHex, String stripeHex, int width,
                                          int height, long seed) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = Color.decode(backgroundHex);
        // Uneven room light: a soft vertical gradient on the background.
        g.setPaint(new GradientPaint(0, 0, shade(bg, 1.06), 0, height, shade(bg, 0.88)));
        g.fillRect(0, 0, width, height);
        // A few folds / wood planks.
        Random random = new Random(seed);
        g.setStroke(new BasicStroke(Math.max(2, width / 120f)));
        for (int i = 0; i < 6; i++) {
            g.setColor(shade(bg, 0.9 + random.nextDouble() * 0.15));
            int y = random.nextInt(height);
            g.drawLine(0, y, width, y + random.nextInt(height / 5) - height / 10);
        }
        // Garment: a shirt-like silhouette with side shading.
        Color garment = Color.decode(garmentHex);
        Path2D shape = shirt(width, height);
        g.setPaint(new GradientPaint(width * 0.2f, 0, shade(garment, 1.08), width * 0.8f, 0, shade(garment, 0.86)));
        g.fill(shape);
        if (stripeHex != null) {
            g.setClip(shape);
            g.setColor(Color.decode(stripeHex));
            int stripe = Math.max(4, height / 22);
            for (int y = 0; y < height; y += stripe * 2) {
                g.fillRect(0, y, width, stripe);
            }
            g.setClip(null);
        }
        g.dispose();
        addNoise(img, random, 6);
        return img;
    }

    /**
     * A small object (a pair of shoes) on a wooden floor made of planks in several tones – the hard
     * case where leftover background can outweigh the garment.
     */
    public static BufferedImage shoesOnPlanks(String plankHex, String shoeHex, int width, int height, long seed) {
        return shoesOnPlanks(plankHex, shoeHex, width, height, seed, 0.5);
    }

    /** @param centerY vertical position of the shoes (0..1); phone photos often have them below center. */
    public static BufferedImage shoesOnPlanks(String plankHex, String shoeHex, int width, int height, long seed,
                                              double centerY) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random random = new Random(seed);
        Color plank = Color.decode(plankHex);
        int plankWidth = width / 6;
        for (int x = 0; x < width; x += plankWidth) {
            Color c = shade(plank, 0.8 + random.nextDouble() * 0.4);
            g.setColor(c);
            g.fillRect(x, 0, plankWidth, height);
            for (int i = 0; i < 20; i++) {
                g.setColor(shade(c, 0.85 + random.nextDouble() * 0.25));
                int gx = x + random.nextInt(plankWidth);
                g.drawLine(gx, 0, gx + random.nextInt(9) - 4, height);
            }
            g.setColor(shade(plank, 0.55));
            g.fillRect(x, 0, 3, height);
        }
        Color shoe = Color.decode(shoeHex);
        for (double cx : new double[] {0.36, 0.64}) {
            int x = (int) (cx * width);
            int y = (int) (centerY * height);
            g.setPaint(new GradientPaint(x - width * 0.1f, 0, shade(shoe, 1.1), x + width * 0.1f, 0, shade(shoe, 0.85)));
            g.fillRoundRect(x - width / 10, y - height / 9, width / 5, height / 3, width / 10, width / 10);
        }
        g.dispose();
        addNoise(img, random, 6);
        return img;
    }

    /** Close-up where the garment fills the whole frame. */
    public static BufferedImage closeUp(String garmentHex, int width, int height, long seed) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Color c = Color.decode(garmentHex);
        g.setPaint(new GradientPaint(0, 0, shade(c, 1.07), width, height, shade(c, 0.9)));
        g.fillRect(0, 0, width, height);
        g.dispose();
        addNoise(img, new Random(seed), 5);
        return img;
    }

    private static Path2D shirt(int w, int h) {
        Path2D p = new Path2D.Double();
        p.moveTo(w * 0.36, h * 0.16);
        p.lineTo(w * 0.64, h * 0.16);
        p.lineTo(w * 0.86, h * 0.30);
        p.lineTo(w * 0.78, h * 0.42);
        p.lineTo(w * 0.70, h * 0.37);
        p.lineTo(w * 0.70, h * 0.86);
        p.lineTo(w * 0.30, h * 0.86);
        p.lineTo(w * 0.30, h * 0.37);
        p.lineTo(w * 0.22, h * 0.42);
        p.lineTo(w * 0.14, h * 0.30);
        p.closePath();
        return p;
    }

    private static Color shade(Color c, double factor) {
        return new Color(clamp(c.getRed() * factor), clamp(c.getGreen() * factor), clamp(c.getBlue() * factor));
    }

    private static void addNoise(BufferedImage img, Random random, int amplitude) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int n = random.nextInt(2 * amplitude + 1) - amplitude;
                int r = clamp(((rgb >> 16) & 0xFF) + n);
                int gr = clamp(((rgb >> 8) & 0xFF) + n);
                int b = clamp((rgb & 0xFF) + n);
                img.setRGB(x, y, (r << 16) | (gr << 8) | b);
            }
        }
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
