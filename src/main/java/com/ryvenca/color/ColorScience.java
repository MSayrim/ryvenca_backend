package com.ryvenca.color;

import java.util.Locale;

/** sRGB ↔ CIELAB conversion and CIEDE2000 color difference. */
public final class ColorScience {

    private static final double XN = 0.95047;
    private static final double YN = 1.00000;
    private static final double ZN = 1.08883;
    private static final double[] SRGB_TO_LINEAR = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double c = i / 255.0;
            SRGB_TO_LINEAR[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    private ColorScience() {
    }

    public static Lab rgbToLab(int r, int g, int b) {
        double rl = SRGB_TO_LINEAR[r];
        double gl = SRGB_TO_LINEAR[g];
        double bl = SRGB_TO_LINEAR[b];
        double x = (rl * 0.4124564 + gl * 0.3575761 + bl * 0.1804375) / XN;
        double y = (rl * 0.2126729 + gl * 0.7151522 + bl * 0.0721750) / YN;
        double z = (rl * 0.0193339 + gl * 0.1191920 + bl * 0.9503041) / ZN;
        double fx = f(x);
        double fy = f(y);
        double fz = f(z);
        return new Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz));
    }

    public static Lab rgbToLab(int rgb) {
        return rgbToLab((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static int labToRgb(Lab lab) {
        double fy = (lab.l() + 16) / 116;
        double fx = fy + lab.a() / 500;
        double fz = fy - lab.b() / 200;
        double x = fInv(fx) * XN;
        double y = fInv(fy) * YN;
        double z = fInv(fz) * ZN;
        double rl = x * 3.2404542 + y * -1.5371385 + z * -0.4985314;
        double gl = x * -0.9692660 + y * 1.8760108 + z * 0.0415560;
        double bl = x * 0.0556434 + y * -0.2040259 + z * 1.0572252;
        return (toSrgb(rl) << 16) | (toSrgb(gl) << 8) | toSrgb(bl);
    }

    public static Lab hexToLab(String hex) {
        return rgbToLab(parseHex(hex));
    }

    public static String labToHex(Lab lab) {
        return toHex(labToRgb(lab));
    }

    public static int parseHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        return Integer.parseInt(h, 16);
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    public static boolean isHex(String value) {
        return value != null && value.matches("#?[0-9a-fA-F]{6}");
    }

    /** Squared Euclidean distance in LAB (CIE76²) – cheap, for clustering. */
    public static double distanceSq(double l1, double a1, double b1, double l2, double a2, double b2) {
        double dl = l1 - l2;
        double da = a1 - a2;
        double db = b1 - b2;
        return dl * dl + da * da + db * db;
    }

    /** CIEDE2000 color difference (Sharma, Wu, Dalal 2005). */
    public static double deltaE2000(Lab c1, Lab c2) {
        double l1 = c1.l();
        double a1 = c1.a();
        double b1 = c1.b();
        double l2 = c2.l();
        double a2 = c2.a();
        double b2 = c2.b();

        double cBar = (Math.hypot(a1, b1) + Math.hypot(a2, b2)) / 2.0;
        double cBar7 = Math.pow(cBar, 7);
        double g = 0.5 * (1 - Math.sqrt(cBar7 / (cBar7 + 6103515625.0))); // 25^7
        double a1p = (1 + g) * a1;
        double a2p = (1 + g) * a2;
        double c1p = Math.hypot(a1p, b1);
        double c2p = Math.hypot(a2p, b2);
        double h1p = hueDegrees(b1, a1p);
        double h2p = hueDegrees(b2, a2p);

        double dLp = l2 - l1;
        double dCp = c2p - c1p;
        double dhp;
        if (c1p * c2p == 0) {
            dhp = 0;
        } else if (Math.abs(h2p - h1p) <= 180) {
            dhp = h2p - h1p;
        } else if (h2p - h1p > 180) {
            dhp = h2p - h1p - 360;
        } else {
            dhp = h2p - h1p + 360;
        }
        double dHp = 2 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dhp / 2));

        double lBarP = (l1 + l2) / 2;
        double cBarP = (c1p + c2p) / 2;
        double hBarP;
        if (c1p * c2p == 0) {
            hBarP = h1p + h2p;
        } else if (Math.abs(h1p - h2p) <= 180) {
            hBarP = (h1p + h2p) / 2;
        } else if (h1p + h2p < 360) {
            hBarP = (h1p + h2p + 360) / 2;
        } else {
            hBarP = (h1p + h2p - 360) / 2;
        }

        double t = 1
                - 0.17 * Math.cos(Math.toRadians(hBarP - 30))
                + 0.24 * Math.cos(Math.toRadians(2 * hBarP))
                + 0.32 * Math.cos(Math.toRadians(3 * hBarP + 6))
                - 0.20 * Math.cos(Math.toRadians(4 * hBarP - 63));
        double dTheta = 30 * Math.exp(-Math.pow((hBarP - 275) / 25, 2));
        double cBarP7 = Math.pow(cBarP, 7);
        double rc = 2 * Math.sqrt(cBarP7 / (cBarP7 + 6103515625.0));
        double lBarMinus50Sq = (lBarP - 50) * (lBarP - 50);
        double sl = 1 + (0.015 * lBarMinus50Sq) / Math.sqrt(20 + lBarMinus50Sq);
        double sc = 1 + 0.045 * cBarP;
        double sh = 1 + 0.015 * cBarP * t;
        double rt = -Math.sin(Math.toRadians(2 * dTheta)) * rc;

        double lTerm = dLp / sl;
        double cTerm = dCp / sc;
        double hTerm = dHp / sh;
        return Math.sqrt(lTerm * lTerm + cTerm * cTerm + hTerm * hTerm + rt * cTerm * hTerm);
    }

    /** Smallest angle between two hues in degrees [0, 180]. */
    public static double hueDistance(double h1, double h2) {
        double d = Math.abs(h1 - h2) % 360;
        return d > 180 ? 360 - d : d;
    }

    private static double hueDegrees(double b, double ap) {
        if (b == 0 && ap == 0) {
            return 0;
        }
        double h = Math.toDegrees(Math.atan2(b, ap));
        return h < 0 ? h + 360 : h;
    }

    private static double f(double t) {
        return t > 216.0 / 24389.0 ? Math.cbrt(t) : (24389.0 / 27.0 * t + 16) / 116;
    }

    private static double fInv(double t) {
        double t3 = t * t * t;
        return t3 > 216.0 / 24389.0 ? t3 : (116 * t - 16) / (24389.0 / 27.0);
    }

    private static int toSrgb(double linear) {
        double c = linear <= 0.0031308 ? 12.92 * linear : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
        return (int) Math.round(Math.max(0, Math.min(1, c)) * 255);
    }
}
