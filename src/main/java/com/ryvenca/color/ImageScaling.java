package com.ryvenca.color;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** High quality downscaling helpers (progressive bilinear halving). */
public final class ImageScaling {

    private ImageScaling() {
    }

    /** Scales so that the longest edge is at most {@code maxEdge}; never upscales. */
    public static BufferedImage fitWithin(BufferedImage src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, maxEdge / (double) Math.max(w, h));
        if (scale >= 1.0) {
            return ensureRgb(src);
        }
        return resize(src, Math.max(1, (int) Math.round(w * scale)), Math.max(1, (int) Math.round(h * scale)));
    }

    public static BufferedImage resize(BufferedImage src, int targetW, int targetH) {
        BufferedImage current = ensureRgb(src);
        int w = current.getWidth();
        int h = current.getHeight();
        while (w / 2 >= targetW && h / 2 >= targetH) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h);
        }
        if (w != targetW || h != targetH) {
            current = draw(current, targetW, targetH);
        }
        return current;
    }

    public static BufferedImage ensureRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static BufferedImage draw(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
