package com.ryvenca.catalog;

public enum StylePreference implements Labeled {
    CASUAL("Casual", "Rahat ve zahmetsiz günlük parçalar"),
    SMART_CASUAL("Smart Casual", "Rahat ama özenli, şık dokunuşlar"),
    MINIMAL("Minimal", "Sade kesimler, nötr renkler"),
    CLASSIC("Classic", "Zamansız, klasik parçalar"),
    STREETWEAR("Streetwear", "Şehirli, rahat ve iddialı"),
    BUSINESS("Business", "Profesyonel ve resmi görünüm"),
    SPORT("Sport", "Konforlu ve hareketli");

    private final String label;
    private final String description;

    StylePreference(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @Override
    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
