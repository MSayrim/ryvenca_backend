package com.ryvenca.garment;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.common.EnumSetConverter;

import jakarta.persistence.Converter;

@Converter
public class OccasionSetConverter extends EnumSetConverter<Occasion> {

    public OccasionSetConverter() {
        super(Occasion.class);
    }
}
