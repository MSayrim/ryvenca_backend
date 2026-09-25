package com.ryvenca.catalog;

public enum WardrobeType implements Labeled {
    WOMEN, MEN, UNISEX;

    @Override
    public String labelKey() {
        return "wardrobeType." + name();
    }
}
