package com.ryvenca.user;

import com.ryvenca.catalog.StylePreference;
import com.ryvenca.common.EnumSetConverter;

import jakarta.persistence.Converter;

@Converter
public class StyleSetConverter extends EnumSetConverter<StylePreference> {

    public StyleSetConverter() {
        super(StylePreference.class);
    }
}
