package com.ryvenca.catalog;

/** Coarse fabric feel derived from the garment type; used for the "Doku Uyumu" explanation. */
public enum Texture implements Labeled {
    JERSEY, KNIT, CRISP_COTTON, DENIM, TAILORED, LEATHER, FLUID, TECHNICAL;

    @Override
    public String labelKey() {
        return "texture." + name();
    }
}
