package com.ryvenca.catalog;

public enum Category implements Labeled {
    TOP("Üst", "Üstler"),
    BOTTOM("Alt", "Altlar"),
    OUTERWEAR("Ceket / Dış Giyim", "Dış Giyim"),
    DRESS("Elbise", "Elbiseler"),
    SHOES("Ayakkabı", "Ayakkabılar"),
    BAG("Çanta", "Çantalar"),
    ACCESSORY("Aksesuar", "Aksesuarlar");

    private final String label;
    private final String pluralLabel;

    Category(String label, String pluralLabel) {
        this.label = label;
        this.pluralLabel = pluralLabel;
    }

    @Override
    public String label() {
        return label;
    }

    public String pluralLabel() {
        return pluralLabel;
    }
}
