package com.ryvenca.garment;

import com.ryvenca.catalog.Season;
import com.ryvenca.common.EnumSetConverter;

import jakarta.persistence.Converter;

@Converter
public class SeasonSetConverter extends EnumSetConverter<Season> {

    public SeasonSetConverter() {
        super(Season.class);
    }
}
