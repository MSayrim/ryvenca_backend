package com.ryvenca.catalog;

public enum Category implements Labeled {
    TOP, BOTTOM, OUTERWEAR, DRESS, SHOES, BAG, ACCESSORY;

    @Override
    public String labelKey() {
        return "category." + name();
    }

    public String pluralLabelKey() {
        return "category." + name() + ".plural";
    }
}
