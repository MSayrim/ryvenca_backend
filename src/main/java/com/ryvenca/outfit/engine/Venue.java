package com.ryvenca.outfit.engine;

import com.ryvenca.catalog.Labeled;

/** Concrete places an outfit suits ("Uygun Ortamlar"). */
public enum Venue implements Labeled {
    OFFICE, MEETING, DINNER, PARTY, COFFEE, SHOPPING, WEEKEND_TRIP, BRUNCH, GYM, WALK, TRAVEL;

    @Override
    public String labelKey() {
        return "venue." + name();
    }
}
