package com.ryvenca.color;

/** CIELAB color (D65). */
public record Lab(double l, double a, double b) {

    public double chroma() {
        return Math.hypot(a, b);
    }

    /** Hue angle in degrees [0, 360). */
    public double hue() {
        double h = Math.toDegrees(Math.atan2(b, a));
        return h < 0 ? h + 360 : h;
    }

    public static Lab ofHex(String hex) {
        return ColorScience.hexToLab(hex);
    }

    public String toHex() {
        return ColorScience.labToHex(this);
    }
}
