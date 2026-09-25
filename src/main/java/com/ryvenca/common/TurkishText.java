package com.ryvenca.common;

import java.util.List;
import java.util.Locale;

/** Small helpers for building natural Turkish sentences. */
public final class TurkishText {

    public static final Locale TR = Locale.forLanguageTag("tr-TR");

    private TurkishText() {
    }

    public static String lower(String s) {
        return s.toLowerCase(TR);
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(TR) + s.substring(1);
    }

    /** "a", "a ve b", "a, b ve c". */
    public static String joinAnd(List<String> parts) {
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " ve " + parts.getLast();
    }
}
