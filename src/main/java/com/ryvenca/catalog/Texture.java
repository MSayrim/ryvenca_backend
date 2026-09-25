package com.ryvenca.catalog;

/** Coarse fabric feel derived from the garment type; used for the "Doku Uyumu" explanation. */
public enum Texture {
    JERSEY("pamuklu örme"),
    KNIT("triko"),
    CRISP_COTTON("pamuklu dokuma"),
    DENIM("denim"),
    TAILORED("kumaş"),
    LEATHER("deri"),
    FLUID("akışkan kumaş"),
    TECHNICAL("teknik kumaş");

    private final String label;

    Texture(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
