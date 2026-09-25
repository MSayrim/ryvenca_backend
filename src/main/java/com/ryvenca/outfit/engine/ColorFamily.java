package com.ryvenca.outfit.engine;

import com.ryvenca.color.ColorName;

/** Color families used to recognize tonal ("ton sür ton") and monochrome outfits. */
enum ColorFamily {
    ACHROMATIC, WARM_NEUTRAL, BLUE, GREEN, RED, PURPLE, ORANGE, YELLOW;

    static ColorFamily of(ColorName color) {
        return switch (color) {
            case BLACK, WHITE, GRAY -> ACHROMATIC;
            case CREAM, BEIGE, BROWN -> WARM_NEUTRAL;
            case NAVY, BLUE, LIGHT_BLUE -> BLUE;
            case GREEN, OLIVE -> GREEN;
            case RED, BURGUNDY, PINK -> RED;
            case PURPLE -> PURPLE;
            case ORANGE -> ORANGE;
            case YELLOW -> YELLOW;
        };
    }
}
