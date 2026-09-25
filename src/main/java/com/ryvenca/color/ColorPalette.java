package com.ryvenca.color;

import java.util.List;

/** A curated color combination; swatches are compared with CIEDE2000 so near colors also match. */
public record ColorPalette(String id, String name, List<String> colors, List<Lab> swatches) {
}
