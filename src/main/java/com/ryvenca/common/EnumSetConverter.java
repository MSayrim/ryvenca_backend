package com.ryvenca.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Stores a set of enum constants as a comma separated string column, in declaration order. */
public abstract class EnumSetConverter<E extends Enum<E>> implements jakarta.persistence.AttributeConverter<Set<E>, String> {

    private final Class<E> type;

    protected EnumSetConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(Set<E> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(values).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<E> convertToEntityAttribute(String column) {
        EnumSet<E> result = EnumSet.noneOf(type);
        if (column == null || column.isBlank()) {
            return result;
        }
        Arrays.stream(column.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> result.add(Enum.valueOf(type, s)));
        return result;
    }
}
