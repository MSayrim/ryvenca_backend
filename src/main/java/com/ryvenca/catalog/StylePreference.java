package com.ryvenca.catalog;

public enum StylePreference implements Labeled {
    CASUAL, SMART_CASUAL, MINIMAL, CLASSIC, STREETWEAR, BUSINESS, SPORT;

    @Override
    public String labelKey() {
        return "style." + name();
    }

    public String descriptionKey() {
        return "style." + name() + ".description";
    }
}
