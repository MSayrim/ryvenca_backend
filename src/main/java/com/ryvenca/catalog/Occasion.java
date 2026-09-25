package com.ryvenca.catalog;

public enum Occasion implements Labeled {
    DAILY(2.0, 3.5),
    OFFICE(3.5, 5.0),
    EVENING(3.4, 5.0),
    WEEKEND(1.5, 3.0),
    SPORT(1.0, 1.8);

    private final double minFormality;
    private final double maxFormality;

    Occasion(double minFormality, double maxFormality) {
        this.minFormality = minFormality;
        this.maxFormality = maxFormality;
    }

    @Override
    public String labelKey() {
        return "occasion." + name();
    }

    public double minFormality() {
        return minFormality;
    }

    public double maxFormality() {
        return maxFormality;
    }
}
