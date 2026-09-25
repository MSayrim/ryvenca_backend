package com.ryvenca.color;

import java.util.List;

import com.ryvenca.catalog.Labeled;

/**
 * Standard color classes a garment is mapped to. Each class has a canonical swatch and several
 * prototypes covering the real-world spread of that color name (e.g. washed vs. raw denim for BLUE).
 */
public enum ColorName implements Labeled {
    BLACK("Siyah", "#1C1C1C", ColorTone.NEUTRAL,
            "#111111", "#1E1E1E", "#2A2A2A", "#1A1C22", "#242021"),
    WHITE("Beyaz", "#F5F5F2", ColorTone.NEUTRAL,
            "#FFFFFF", "#F4F4F2", "#ECEEF0", "#E6E8EA", "#DADAD7"),
    GRAY("Gri", "#8E8E8E", ColorTone.NEUTRAL,
            "#4A4C4F", "#5E5E5E", "#808080", "#9B9B9B", "#B4B4B4", "#C8C8C6", "#6B6E73"),
    CREAM("Krem", "#EFE6D2", ColorTone.NEUTRAL,
            "#F3EBDD", "#EDE3CC", "#E6DCC6", "#F5EEDF", "#E9DFCB"),
    BEIGE("Bej", "#CDB89A", ColorTone.NEUTRAL,
            "#D8C6A8", "#C9B28E", "#BFA88A", "#D2BFA3", "#B99F7B", "#C19A6B", "#CBB9A0"),
    BROWN("Kahverengi", "#7A5234", ColorTone.NEUTRAL,
            "#7A5234", "#5C3A21", "#8B5A2B", "#9C6B3F", "#4A3222", "#6F4E37", "#3B2A20"),
    NAVY("Lacivert", "#1F2A44", ColorTone.NEUTRAL,
            "#1F2A44", "#1B2440", "#25324F", "#2C3A5A", "#202637"),
    BLUE("Mavi", "#3A5F9E", ColorTone.ACCENT,
            "#3A5F9E", "#4A6FA5", "#5B7DB1", "#2F4F7F", "#3D6FB6", "#6A87A8", "#44597A", "#5A6F8F"),
    LIGHT_BLUE("Açık Mavi", "#A9C6E3", ColorTone.SOFT,
            "#A9C6E3", "#B8D0E6", "#8FB3D9", "#C9DAEA", "#9DB7CF"),
    GREEN("Yeşil", "#3F7A4A", ColorTone.ACCENT,
            "#3F7A4A", "#2E6B3F", "#5E9C6A", "#1F4D33", "#9CAF88", "#A8D5BA", "#2F5D50"),
    OLIVE("Haki", "#6B6B3A", ColorTone.EARTH,
            "#6B6B3A", "#5A5A32", "#7A7A4A", "#4B4F2E", "#8A8660", "#6E6A4B"),
    RED("Kırmızı", "#B8272E", ColorTone.ACCENT,
            "#B8272E", "#C8102E", "#D63A3A", "#A51F2A", "#E0474C"),
    BURGUNDY("Bordo", "#6D1F2B", ColorTone.EARTH,
            "#6D1F2B", "#5A1A24", "#7B2433", "#800020", "#4E1A22"),
    ORANGE("Turuncu", "#D9772B", ColorTone.ACCENT,
            "#D9772B", "#E07B39", "#C8612B", "#B5562A", "#F0A060", "#E8894A"),
    YELLOW("Sarı", "#E3C442", ColorTone.ACCENT,
            "#E3C442", "#F2D54B", "#D8B53A", "#C9A227", "#F3E08A"),
    PINK("Pembe", "#E8AFC0", ColorTone.SOFT,
            "#E8AFC0", "#F4C2C2", "#D98FA5", "#E75480", "#D7A9A0", "#F2D0D6"),
    PURPLE("Mor", "#6E4A8E", ColorTone.ACCENT,
            "#6E4A8E", "#5B3A7A", "#8E6BB0", "#B39DDB", "#4B2E5E");

    private final String label;
    private final String hex;
    private final ColorTone tone;
    private final Lab lab;
    private final List<Lab> prototypes;

    ColorName(String label, String hex, ColorTone tone, String... prototypeHexes) {
        this.label = label;
        this.hex = hex;
        this.tone = tone;
        this.lab = ColorScience.hexToLab(hex);
        this.prototypes = java.util.Arrays.stream(prototypeHexes).map(ColorScience::hexToLab).toList();
    }

    @Override
    public String label() {
        return label;
    }

    public String hex() {
        return hex;
    }

    public ColorTone tone() {
        return tone;
    }

    public Lab lab() {
        return lab;
    }

    public List<Lab> prototypes() {
        return prototypes;
    }

    public boolean isNeutral() {
        return tone == ColorTone.NEUTRAL;
    }
}
