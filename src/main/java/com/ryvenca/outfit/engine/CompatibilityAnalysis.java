package com.ryvenca.outfit.engine;

import java.util.List;

import com.ryvenca.catalog.Texture;

/**
 * @param score          0..1 garment/category compatibility
 * @param formality      weighted average formality of the outfit (1..5)
 * @param textures       distinct fabric feels of the core pieces
 * @param pairing        the most characteristic good pairing, if any
 * @param pairingItems   the pieces that form {@link #pairing}
 * @param highLow        a formal and a casual piece mixed on purpose (smart casual)
 * @param mismatch       the two pieces with a problematic formality gap, if any
 */
public record CompatibilityAnalysis(double score, double formality, List<Texture> textures, Pairing pairing,
                                    List<WardrobeItem> pairingItems, List<WardrobeItem> highLow,
                                    List<WardrobeItem> mismatch) {

    public enum Pairing {
        SUIT, SMART_MIX, CLASSIC_BASE, KNIT_CONTRAST, EDGY_CONTRAST, EASY_CLASSIC, TRENCH
    }
}
