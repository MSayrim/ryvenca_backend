package com.ryvenca.catalog;

public enum Occasion implements Labeled {
    DAILY("Günlük", 2.0, 3.5),
    OFFICE("Ofis", 3.5, 5.0),
    EVENING("Akşam", 3.4, 5.0),
    WEEKEND("Hafta Sonu", 1.5, 3.0),
    SPORT("Spor", 1.0, 1.8);

    private final String label;
    private final double minFormality;
    private final double maxFormality;

    Occasion(String label, double minFormality, double maxFormality) {
        this.label = label;
        this.minFormality = minFormality;
        this.maxFormality = maxFormality;
    }

    @Override
    public String label() {
        return label;
    }

    public double minFormality() {
        return minFormality;
    }

    public double maxFormality() {
        return maxFormality;
    }
}
