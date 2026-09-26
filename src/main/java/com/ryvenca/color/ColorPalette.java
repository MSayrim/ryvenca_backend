package com.ryvenca.color;

import java.util.List;

/**
 * A curated color combination; swatches are compared with CIEDE2000 so near colors also match. The
 * localized name is the message {@code palette.<id>}. The first swatch is the signature color.
 */
public record ColorPalette(String id, List<String> colors, List<Lab> swatches) {

    public static ColorPalette of(String id, List<String> colors) {
        return new ColorPalette(id, List.copyOf(colors), colors.stream().map(ColorScience::hexToLab).toList());
    }
}
