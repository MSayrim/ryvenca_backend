package com.ryvenca.catalog;

import java.time.LocalDate;

public enum Season implements Labeled {
    SPRING, SUMMER, AUTUMN, WINTER;

    @Override
    public String labelKey() {
        return "season." + name();
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
