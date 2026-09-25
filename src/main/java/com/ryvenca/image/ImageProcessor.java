package com.ryvenca.image;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.ryvenca.color.ImageScaling;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;

/**
 * Turns an ordinary phone photo into the versions the apps display. No background removal: the
 * photo stays the user's own, we only fix orientation, size and apply a gentle tonal normalization.
 */
@Component
public class ImageProcessor {

    public static final int ORIGINAL_MAX_EDGE = 2400;
    public static final int DISPLAY_MAX_EDGE = 1440;
    public static final int THUMB_WIDTH = 480;
    public static final int THUMB_HEIGHT = 600;
    private static final long MAX_PIXELS = 50_000_000L;

    /** Decodes JPEG/PNG/WebP, applies EXIF orientation and flattens to RGB. */
    public BufferedImage decode(byte[] bytes) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            image = null;
        }
        if (image == null) {
            throw new ApiException(ErrorCode.INVALID_IMAGE,
                    "Bu dosya okunamadı. Lütfen JPEG, PNG veya WebP formatında bir fotoğraf seç.");
        }
        if ((long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
            throw new ApiException(ErrorCode.INVALID_IMAGE, "Fotoğraf çözünürlüğü çok yüksek.");
        }
        if (image.getWidth() < 64 || image.getHeight() < 64) {
            throw new ApiException(ErrorCode.INVALID_IMAGE, "Fotoğraf çok küçük. Daha net bir fotoğraf dene.");
        }
        return applyOrientation(ImageScaling.ensureRgb(image), readOrientation(bytes));
    }

    public BufferedImage original(BufferedImage image) {
        return ImageScaling.fitWithin(image, ORIGINAL_MAX_EDGE);
    }

    public BufferedImage display(BufferedImage image) {
        return normalize(ImageScaling.fitWithin(image, DISPLAY_MAX_EDGE));
    }

    /** 4:5 cover crop centered on the detected garment (focus point), then normalized. */
    public BufferedImage thumbnail(BufferedImage image, double focusX, double focusY) {
        int w = image.getWidth();
        int h = image.getHeight();
        double targetRatio = THUMB_WIDTH / (double) THUMB_HEIGHT;
        int cropW;
        int cropH;
        if (w / (double) h > targetRatio) {
            cropH = h;
            cropW = (int) Math.round(h * targetRatio);
        } else {
            cropW = w;
            cropH = (int) Math.round(w / targetRatio);
        }
        int x = clamp((int) Math.round(focusX * w - cropW / 2.0), 0, w - cropW);
        int y = clamp((int) Math.round(focusY * h - cropH / 2.0), 0, h - cropH);
        BufferedImage crop = image.getSubimage(x, y, cropW, cropH);
        return normalize(ImageScaling.resize(crop, THUMB_WIDTH, THUMB_HEIGHT));
    }

    public byte[] toJpeg(BufferedImage image, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /**
     * Gentle brightness/contrast normalization: stretches the 1st–99th luminance percentiles towards
     * the full range (blended at 45% so the photo keeps its character) and lifts very dark photos.
     */
    static BufferedImage normalize(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        int[] histogram = new int[256];
        long sum = 0;
        for (int p : pixels) {
            int lum = luminance(p);
            histogram[lum]++;
            sum += lum;
        }
        int lo = percentile(histogram, pixels.length, 0.01);
        int hi = percentile(histogram, pixels.length, 0.99);
        double mean = sum / (double) pixels.length;
        boolean stretch = hi - lo < 235 && hi - lo > 40;
        double gamma = mean < 85 ? 0.85 : 1.0;
        if (!stretch && gamma == 1.0) {
            return image;
        }
        int[] lut = new int[256];
        for (int v = 0; v < 256; v++) {
            double out = v;
            if (stretch) {
                double stretched = (v - lo) * 255.0 / (hi - lo);
                out = 0.55 * v + 0.45 * Math.max(0, Math.min(255, stretched));
            }
            if (gamma != 1.0) {
                out = 255 * Math.pow(out / 255.0, gamma);
            }
            lut[v] = (int) Math.round(Math.max(0, Math.min(255, out)));
        }
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            pixels[i] = (lut[(p >> 16) & 0xFF] << 16) | (lut[(p >> 8) & 0xFF] << 8) | lut[p & 0xFF];
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    private static int luminance(int rgb) {
        return (int) Math.round(0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF));
    }

    private static int percentile(int[] histogram, int total, double q) {
        long target = Math.round(total * q);
        long acc = 0;
        for (int v = 0; v < 256; v++) {
            acc += histogram[v];
            if (acc >= target) {
                return v;
            }
        }
        return 255;
    }

    private static int readOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
            // No or unreadable EXIF: keep the image as decoded.
        }
        return 1;
    }

    static BufferedImage applyOrientation(BufferedImage image, int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                t.scale(-1, 1);
                t.translate(-w, 0);
            }
            case 3 -> {
                t.translate(w, h);
                t.rotate(Math.PI);
            }
            case 4 -> {
                t.scale(1, -1);
                t.translate(0, -h);
            }
            case 5 -> {
                t.rotate(-Math.PI / 2);
                t.scale(-1, 1);
            }
            case 6 -> {
                t.translate(h, 0);
                t.rotate(Math.PI / 2);
            }
            case 7 -> {
                t.scale(-1, 1);
                t.translate(-h, 0);
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
            }
            case 8 -> {
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
            }
            default -> {
                return image;
            }
        }
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, t, null);
        g.dispose();
        return out;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
