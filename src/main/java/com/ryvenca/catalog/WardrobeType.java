package com.ryvenca.catalog;

public enum WardrobeType implements Labeled {
    WOMEN("Kadın"),
    MEN("Erkek"),
    UNISEX("Unisex");

    private final String label;

    WardrobeType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
