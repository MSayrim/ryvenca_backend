package com.ryvenca.catalog;

/** Enum whose display label is localized through the message files ({@link #labelKey()}). */
public interface Labeled {

    String name();

    String labelKey();
}
