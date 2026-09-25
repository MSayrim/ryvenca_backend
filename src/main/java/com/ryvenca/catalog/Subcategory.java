package com.ryvenca.catalog;

import static com.ryvenca.catalog.Occasion.DAILY;
import static com.ryvenca.catalog.Occasion.EVENING;
import static com.ryvenca.catalog.Occasion.OFFICE;
import static com.ryvenca.catalog.Occasion.WEEKEND;
import static com.ryvenca.catalog.Season.AUTUMN;
import static com.ryvenca.catalog.Season.SPRING;
import static com.ryvenca.catalog.Season.SUMMER;
import static com.ryvenca.catalog.Season.WINTER;
import static com.ryvenca.catalog.StylePreference.BUSINESS;
import static com.ryvenca.catalog.StylePreference.CASUAL;
import static com.ryvenca.catalog.StylePreference.CLASSIC;
import static com.ryvenca.catalog.StylePreference.MINIMAL;
import static com.ryvenca.catalog.StylePreference.SMART_CASUAL;
import static com.ryvenca.catalog.StylePreference.STREETWEAR;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Garment types with the fashion knowledge the outfit engine relies on.
 *
 * <p>formality: 1 = sport/lounge … 5 = formal. warmth: 0 = light (summer) … 3 = heavy (winter coat).
 */
public enum Subcategory implements Labeled {

    // Üst
    T_SHIRT(Category.TOP, 2.0, Texture.JERSEY, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR, MINIMAL, SMART_CASUAL, StylePreference.SPORT)),
    TANK_TOP(Category.TOP, 1.5, Texture.JERSEY, 0,
            seasons(SPRING, SUMMER), occasions(DAILY, WEEKEND, Occasion.SPORT), styles(CASUAL, StylePreference.SPORT)),
    POLO(Category.TOP, 2.6, Texture.JERSEY, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, WEEKEND, OFFICE), styles(SMART_CASUAL, CASUAL, CLASSIC)),
    SHIRT(Category.TOP, 4.0, Texture.CRISP_COTTON, 1,
            allSeasons(), occasions(OFFICE, DAILY, EVENING), styles(CLASSIC, BUSINESS, SMART_CASUAL, MINIMAL)),
    BLOUSE(Category.TOP, 3.6, Texture.FLUID, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(OFFICE, EVENING, DAILY), styles(CLASSIC, BUSINESS, SMART_CASUAL, MINIMAL)),
    SWEATER(Category.TOP, 3.0, Texture.KNIT, 2,
            seasons(AUTUMN, WINTER, SPRING), occasions(DAILY, OFFICE, WEEKEND), styles(SMART_CASUAL, CLASSIC, MINIMAL, CASUAL)),
    SWEATSHIRT(Category.TOP, 1.6, Texture.JERSEY, 1,
            seasons(AUTUMN, WINTER, SPRING), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR, StylePreference.SPORT)),
    HOODIE(Category.TOP, 1.2, Texture.JERSEY, 2,
            seasons(AUTUMN, WINTER, SPRING), occasions(WEEKEND, DAILY, Occasion.SPORT), styles(STREETWEAR, StylePreference.SPORT, CASUAL)),

    // Alt
    JEANS(Category.BOTTOM, 2.5, Texture.DENIM, 1,
            allSeasons(), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR, SMART_CASUAL, MINIMAL)),
    TROUSERS(Category.BOTTOM, 4.4, Texture.TAILORED, 1,
            allSeasons(), occasions(OFFICE, EVENING, DAILY), styles(CLASSIC, BUSINESS, MINIMAL, SMART_CASUAL)),
    CHINOS(Category.BOTTOM, 3.4, Texture.CRISP_COTTON, 1,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, OFFICE, WEEKEND), styles(SMART_CASUAL, CLASSIC, CASUAL)),
    SKIRT(Category.BOTTOM, 3.5, Texture.FLUID, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, OFFICE, EVENING), styles(CLASSIC, MINIMAL, SMART_CASUAL)),
    SHORTS(Category.BOTTOM, 1.6, Texture.CRISP_COTTON, 0,
            seasons(SUMMER, SPRING), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR)),
    SWEATPANTS(Category.BOTTOM, 1.0, Texture.JERSEY, 1,
            seasons(SPRING, AUTUMN, WINTER), occasions(Occasion.SPORT, WEEKEND), styles(StylePreference.SPORT, STREETWEAR)),

    // Ceket / Dış giyim
    BLAZER(Category.OUTERWEAR, 4.5, Texture.TAILORED, 1,
            seasons(SPRING, AUTUMN, WINTER), occasions(OFFICE, EVENING, DAILY), styles(CLASSIC, BUSINESS, SMART_CASUAL, MINIMAL)),
    JACKET(Category.OUTERWEAR, 3.0, Texture.CRISP_COTTON, 1,
            seasons(SPRING, AUTUMN), occasions(DAILY, WEEKEND), styles(CASUAL, SMART_CASUAL, STREETWEAR)),
    DENIM_JACKET(Category.OUTERWEAR, 2.0, Texture.DENIM, 1,
            seasons(SPRING, AUTUMN, SUMMER), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR)),
    LEATHER_JACKET(Category.OUTERWEAR, 2.8, Texture.LEATHER, 1,
            seasons(SPRING, AUTUMN, WINTER), occasions(DAILY, EVENING, WEEKEND), styles(STREETWEAR, CASUAL, SMART_CASUAL)),
    CARDIGAN(Category.OUTERWEAR, 3.0, Texture.KNIT, 1,
            seasons(SPRING, AUTUMN, WINTER), occasions(DAILY, OFFICE, WEEKEND), styles(CASUAL, SMART_CASUAL, MINIMAL, CLASSIC)),
    TRENCH_COAT(Category.OUTERWEAR, 4.0, Texture.CRISP_COTTON, 1,
            seasons(SPRING, AUTUMN), occasions(OFFICE, DAILY, EVENING), styles(CLASSIC, MINIMAL, BUSINESS, SMART_CASUAL)),
    COAT(Category.OUTERWEAR, 4.0, Texture.TAILORED, 3,
            seasons(AUTUMN, WINTER), occasions(OFFICE, DAILY, EVENING), styles(CLASSIC, MINIMAL, BUSINESS, SMART_CASUAL)),
    PUFFER(Category.OUTERWEAR, 1.8, Texture.TECHNICAL, 3,
            seasons(WINTER, AUTUMN), occasions(DAILY, WEEKEND, Occasion.SPORT), styles(CASUAL, STREETWEAR, StylePreference.SPORT)),

    // Elbise
    DAY_DRESS(Category.DRESS, 2.6, Texture.FLUID, 0,
            seasons(SPRING, SUMMER), occasions(DAILY, WEEKEND), styles(CASUAL, MINIMAL, SMART_CASUAL)),
    SHIRT_DRESS(Category.DRESS, 3.5, Texture.CRISP_COTTON, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, OFFICE), styles(SMART_CASUAL, CLASSIC, MINIMAL)),
    KNIT_DRESS(Category.DRESS, 3.2, Texture.KNIT, 2,
            seasons(AUTUMN, WINTER), occasions(DAILY, OFFICE, EVENING), styles(MINIMAL, CLASSIC, SMART_CASUAL)),
    SLIP_DRESS(Category.DRESS, 4.0, Texture.FLUID, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(EVENING), styles(MINIMAL, CLASSIC)),
    EVENING_DRESS(Category.DRESS, 5.0, Texture.FLUID, 0,
            allSeasons(), occasions(EVENING), styles(CLASSIC)),
    JUMPSUIT(Category.DRESS, 3.0, Texture.FLUID, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, EVENING, WEEKEND), styles(SMART_CASUAL, MINIMAL, CASUAL)),

    // Ayakkabı
    SNEAKER(Category.SHOES, 2.0, null, 1,
            allSeasons(), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR, SMART_CASUAL, MINIMAL)),
    RUNNING_SHOE(Category.SHOES, 1.0, Texture.TECHNICAL, 1,
            allSeasons(), occasions(Occasion.SPORT, WEEKEND), styles(StylePreference.SPORT, STREETWEAR)),
    LOAFER(Category.SHOES, 4.0, Texture.LEATHER, 1,
            seasons(SPRING, SUMMER, AUTUMN), occasions(OFFICE, DAILY, EVENING), styles(CLASSIC, SMART_CASUAL, BUSINESS, MINIMAL)),
    DRESS_SHOE(Category.SHOES, 5.0, Texture.LEATHER, 1,
            allSeasons(), occasions(OFFICE, EVENING), styles(BUSINESS, CLASSIC)),
    BOOT(Category.SHOES, 3.0, Texture.LEATHER, 2,
            seasons(AUTUMN, WINTER), occasions(DAILY, WEEKEND, EVENING, OFFICE), styles(CASUAL, SMART_CASUAL, STREETWEAR, CLASSIC)),
    HEELS(Category.SHOES, 4.6, null, 1,
            allSeasons(), occasions(EVENING, OFFICE), styles(CLASSIC, BUSINESS)),
    FLATS(Category.SHOES, 3.5, null, 0,
            seasons(SPRING, SUMMER, AUTUMN), occasions(DAILY, OFFICE), styles(CLASSIC, MINIMAL, SMART_CASUAL)),
    SANDAL(Category.SHOES, 2.2, null, 0,
            seasons(SUMMER, SPRING), occasions(DAILY, WEEKEND, EVENING), styles(CASUAL, MINIMAL)),

    // Çanta
    HANDBAG(Category.BAG, 4.0, Texture.LEATHER, 0,
            allSeasons(), occasions(OFFICE, EVENING, DAILY), styles(CLASSIC, BUSINESS, MINIMAL, SMART_CASUAL)),
    SHOULDER_BAG(Category.BAG, 3.4, Texture.LEATHER, 0,
            allSeasons(), occasions(DAILY, OFFICE, EVENING, WEEKEND), styles(SMART_CASUAL, CLASSIC, MINIMAL, CASUAL)),
    CROSSBODY(Category.BAG, 2.4, null, 0,
            allSeasons(), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR, SMART_CASUAL)),
    TOTE(Category.BAG, 3.0, null, 0,
            allSeasons(), occasions(DAILY, OFFICE, WEEKEND), styles(CASUAL, MINIMAL, SMART_CASUAL)),
    CLUTCH(Category.BAG, 4.8, null, 0,
            allSeasons(), occasions(EVENING), styles(CLASSIC)),
    BACKPACK(Category.BAG, 1.5, Texture.TECHNICAL, 0,
            allSeasons(), occasions(DAILY, WEEKEND, Occasion.SPORT), styles(CASUAL, STREETWEAR, StylePreference.SPORT)),

    // Aksesuar
    SUNGLASSES(Category.ACCESSORY, 3.0, null, 0,
            seasons(SPRING, SUMMER), occasions(DAILY, WEEKEND), styles(CASUAL, MINIMAL, CLASSIC, SMART_CASUAL, STREETWEAR)),
    WATCH(Category.ACCESSORY, 3.5, null, 0,
            allSeasons(), occasions(DAILY, OFFICE, EVENING, WEEKEND), styles(CLASSIC, BUSINESS, SMART_CASUAL, MINIMAL)),
    BELT(Category.ACCESSORY, 3.5, Texture.LEATHER, 0,
            allSeasons(), occasions(DAILY, OFFICE, EVENING), styles(CLASSIC, BUSINESS, SMART_CASUAL)),
    SCARF(Category.ACCESSORY, 3.0, null, 1,
            seasons(AUTUMN, WINTER, SPRING), occasions(DAILY, OFFICE, EVENING, WEEKEND), styles(CLASSIC, MINIMAL, SMART_CASUAL)),
    HAT(Category.ACCESSORY, 2.0, null, 0,
            allSeasons(), occasions(DAILY, WEEKEND), styles(CASUAL, STREETWEAR)),
    JEWELRY(Category.ACCESSORY, 3.6, null, 0,
            allSeasons(), occasions(DAILY, OFFICE, EVENING), styles(CLASSIC, MINIMAL, SMART_CASUAL));

    private final Category category;
    private final double formality;
    private final Texture texture;
    private final int warmth;
    private final Set<Season> defaultSeasons;
    private final Set<Occasion> defaultOccasions;
    private final Set<StylePreference> styles;

    Subcategory(Category category, double formality, Texture texture, int warmth,
                Set<Season> defaultSeasons, Set<Occasion> defaultOccasions, Set<StylePreference> styles) {
        this.category = category;
        this.formality = formality;
        this.texture = texture;
        this.warmth = warmth;
        this.defaultSeasons = Collections.unmodifiableSet(defaultSeasons);
        this.defaultOccasions = Collections.unmodifiableSet(defaultOccasions);
        this.styles = Collections.unmodifiableSet(styles);
    }

    public Category category() {
        return category;
    }

    @Override
    public String labelKey() {
        return "subcategory." + name();
    }

    public double formality() {
        return formality;
    }

    /** May be null when the fabric is too varied to say anything useful (e.g. sneakers). */
    public Texture texture() {
        return texture;
    }

    public int warmth() {
        return warmth;
    }

    public Set<Season> defaultSeasons() {
        return defaultSeasons;
    }

    public Set<Occasion> defaultOccasions() {
        return defaultOccasions;
    }

    public Set<StylePreference> styles() {
        return styles;
    }

    public static List<Subcategory> of(Category category) {
        return Arrays.stream(values()).filter(s -> s.category == category).toList();
    }

    private static Set<Season> seasons(Season first, Season... rest) {
        return EnumSet.of(first, rest);
    }

    private static Set<Season> allSeasons() {
        return EnumSet.allOf(Season.class);
    }

    private static Set<Occasion> occasions(Occasion first, Occasion... rest) {
        return EnumSet.of(first, rest);
    }

    private static Set<StylePreference> styles(StylePreference first, StylePreference... rest) {
        return EnumSet.of(first, rest);
    }
}
