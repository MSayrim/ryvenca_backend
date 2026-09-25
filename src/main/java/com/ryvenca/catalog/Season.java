package com.ryvenca.catalog;

import java.time.LocalDate;

public enum Season implements Labeled {
    SPRING("İlkbahar"),
    SUMMER("Yaz"),
    AUTUMN("Sonbahar"),
    WINTER("Kış");

    private final String label;

    Season(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** Northern hemisphere meteorological seasons. */
    public static Season of(LocalDate date) {
        return switch (date.getMonth()) {
            case MARCH, APRIL, MAY -> SPRING;
            case JUNE, JULY, AUGUST -> SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> AUTUMN;
            case DECEMBER, JANUARY, FEBRUARY -> WINTER;
        };
    }
}
