package com.ryvenca.outfit.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.ryvenca.catalog.Subcategory;
import com.ryvenca.color.ColorName;
import com.ryvenca.color.Lab;

/** Test wardrobes built from realistic measured colors. */
final class Wardrobes {

    private Wardrobes() {
    }

    static WardrobeItem item(long id, Subcategory sub, ColorName color, String hex) {
        return new WardrobeItem(id, sub, color, Lab.ofHex(hex), false,
                EnumSet.copyOf(sub.defaultSeasons()), EnumSet.copyOf(sub.defaultOccasions()));
    }

    static WardrobeItem patterned(long id, Subcategory sub, ColorName color, String hex) {
        WardrobeItem i = item(id, sub, color, hex);
        return new WardrobeItem(i.id(), i.subcategory(), i.color(), i.lab(), true, i.seasons(), i.occasions());
    }

    // Classic capsule
    static final WardrobeItem WHITE_SHIRT = item(1, Subcategory.SHIRT, ColorName.WHITE, "#F1F0EC");
    static final WardrobeItem BLACK_TEE = item(2, Subcategory.T_SHIRT, ColorName.BLACK, "#1F1E1F");
    static final WardrobeItem CREAM_SWEATER = item(3, Subcategory.SWEATER, ColorName.CREAM, "#EAE0CB");
    static final WardrobeItem RED_BLOUSE = item(4, Subcategory.BLOUSE, ColorName.RED, "#B8272E");
    static final WardrobeItem NAVY_TROUSERS = item(10, Subcategory.TROUSERS, ColorName.NAVY, "#222B42");
    static final WardrobeItem BLUE_JEANS = item(11, Subcategory.JEANS, ColorName.BLUE, "#4F6A8E");
    static final WardrobeItem BLACK_TROUSERS = item(12, Subcategory.TROUSERS, ColorName.BLACK, "#1E1E20");
    static final WardrobeItem ORANGE_SKIRT = item(13, Subcategory.SKIRT, ColorName.ORANGE, "#D9772B");
    static final WardrobeItem BEIGE_BLAZER = item(20, Subcategory.BLAZER, ColorName.BEIGE, "#C8B596");
    static final WardrobeItem TRENCH = item(21, Subcategory.TRENCH_COAT, ColorName.BEIGE, "#C9B08A");
    static final WardrobeItem PUFFER = item(22, Subcategory.PUFFER, ColorName.BLACK, "#202022");
    static final WardrobeItem WHITE_SNEAKER = item(30, Subcategory.SNEAKER, ColorName.WHITE, "#EEEDEA");
    static final WardrobeItem BROWN_LOAFER = item(31, Subcategory.LOAFER, ColorName.BROWN, "#5E3B24");
    static final WardrobeItem PURPLE_HEELS = item(32, Subcategory.HEELS, ColorName.PURPLE, "#6E4A8E");
    static final WardrobeItem RUNNING = item(33, Subcategory.RUNNING_SHOE, ColorName.GRAY, "#8A8C90");
    static final WardrobeItem BROWN_BAG = item(40, Subcategory.HANDBAG, ColorName.BROWN, "#6B4630");
    static final WardrobeItem SUNGLASSES = item(50, Subcategory.SUNGLASSES, ColorName.BLACK, "#1B1B1B");
    static final WardrobeItem BLACK_DRESS = item(60, Subcategory.SLIP_DRESS, ColorName.BLACK, "#1C1B1D");
    static final WardrobeItem SWEATPANTS = item(14, Subcategory.SWEATPANTS, ColorName.GRAY, "#7D7F83");

    static List<WardrobeItem> capsule() {
        return new ArrayList<>(List.of(WHITE_SHIRT, BLACK_TEE, CREAM_SWEATER, RED_BLOUSE, NAVY_TROUSERS, BLUE_JEANS,
                BLACK_TROUSERS, ORANGE_SKIRT, BEIGE_BLAZER, TRENCH, PUFFER, WHITE_SNEAKER, BROWN_LOAFER, PURPLE_HEELS,
                RUNNING, BROWN_BAG, SUNGLASSES, BLACK_DRESS, SWEATPANTS));
    }
}
