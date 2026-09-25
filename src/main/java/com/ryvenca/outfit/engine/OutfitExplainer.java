package com.ryvenca.outfit.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.StylePreference;
import com.ryvenca.color.ColorName;
import com.ryvenca.i18n.Localizer;
import com.ryvenca.outfit.engine.OutfitStory.Reason;

/**
 * Turns the numeric analyses into localized copy: title, description and the "Neden Uyumlu?"
 * (why it works) cards. Every sentence is a full template in the message files
 * ({@code explain.*}, {@code title.*}); only garment phrases and color lists are filled in.
 */
public final class OutfitExplainer {

    private final Localizer l;

    public OutfitExplainer(Localizer localizer) {
        this.l = localizer;
    }

    public OutfitStory explain(OutfitEvaluation e) {
        return explain(e, Set.of());
    }

    /**
     * @param usedTitles titles already shown in the same list; another fitting variant is chosen when
     *                   possible so two cards next to each other do not carry the same name.
     */
    public OutfitStory explain(OutfitEvaluation e, Set<String> usedTitles) {
        String paletteName = e.color().paletteMatched() ? l.t("palette." + e.color().paletteId()) : null;
        List<Reason> reasons = new ArrayList<>();
        reasons.add(new Reason("COLOR", l.t("reason.COLOR"), colorText(e.color(), paletteName)));
        reasons.add(new Reason("TEXTURE", l.t("reason.TEXTURE"), textureText(e)));
        reasons.add(new Reason("OCCASION", l.t("reason.OCCASION"), occasionText(e.occasion())));
        String season = seasonText(e.season());
        if (season != null) {
            reasons.add(new Reason("SEASON", l.t("reason.SEASON"), season));
        }
        List<String> ids = titleIds(e);
        String id = ids.stream().filter(t -> !usedTitles.contains(l.t("title." + t))).findFirst().orElse(ids.getFirst());
        return new OutfitStory(l.t("title." + id), l.t("title." + id + ".description"), List.copyOf(reasons), paletteName);
    }

    // ---- Renk Dengesi (color balance) -----------------------------------------------------------

    String colorText(ColorAnalysis c, String paletteName) {
        String all = colors(c.colors());
        String text = switch (c.colorCase()) {
            case SINGLE_COLOR -> l.t("explain.color.singleColor", l.inline(c.colors().getFirst()));
            case MONOCHROME -> l.t("explain.color.monochrome", all);
            case TONAL -> l.t("explain.color.tonal", all);
            case ALL_NEUTRAL -> l.t("explain.color.allNeutral", all);
            case SINGLE_ACCENT -> c.neutrals().isEmpty()
                    ? l.t("explain.color.accentOnly", l.inline(c.accents().getFirst()))
                    : l.t("explain.color.singleAccent", colors(c.neutrals()), l.inline(c.accents().getFirst()));
            case SOFT_PAIR -> l.t("explain.color.softPair", colors(c.accents()));
            case ANALOGOUS -> l.t("explain.color.analogous", colors(c.accents()));
            case COMPLEMENTARY -> l.t("explain.color.complementary", colors(c.accents()));
            case DISCORDANT -> l.t("explain.color.discordant", colors(c.accents()));
            case BUSY -> l.t("explain.color.busy", all);
        };
        text = l.capitalize(text);
        if (paletteName != null) {
            text = l.then(text, l.t("explain.color.palette", paletteName));
        }
        if (c.accentEcho()) {
            text = l.then(text, l.t("explain.color.accentEcho"));
        } else if (c.bagShoeEcho()) {
            text = l.then(text, l.t("explain.color.bagShoeEcho"));
        }
        if (c.nearClash()) {
            text = l.then(text, l.t("explain.color.nearClash"));
        }
        if (c.patternClash()) {
            text = l.then(text, l.t("explain.color.patternClash"));
        }
        return text;
    }

    // ---- Doku Uyumu (texture harmony) -----------------------------------------------------------

    String textureText(OutfitEvaluation e) {
        CompatibilityAnalysis c = e.compatibility();
        List<WardrobeItem> p = c.pairingItems();
        String textures = l.join(c.textures().stream().map(l::inline).toList());
        String main;
        if (c.pairing() != null) {
            main = switch (c.pairing()) {
                case SUIT -> l.t("explain.texture.suit", phrase(p.get(0)), phrase(p.get(1)));
                case SMART_MIX -> l.t("explain.texture.smartMix", phrase(p.get(0)), phrase(p.get(1)));
                case EDGY_CONTRAST -> l.t("explain.texture.edgyContrast", phrase(p.get(0)), phrase(p.get(1)));
                case CLASSIC_BASE -> l.t("explain.texture.classicBase", phrase(p.get(0)), phrase(p.get(1)));
                case KNIT_CONTRAST -> l.t("explain.texture.knitContrast", phrase(p.get(0)), phrase(p.get(1)));
                case EASY_CLASSIC -> l.t("explain.texture.easyClassic");
                case TRENCH -> l.t("explain.texture.trench", phrase(p.get(0)));
            };
            main = l.capitalize(main);
            if (c.textures().size() >= 2 && c.pairing() != CompatibilityAnalysis.Pairing.EASY_CLASSIC) {
                main = l.then(main, l.t("explain.texture.variety", textures));
            }
        } else if (c.textures().size() >= 2) {
            List<String> names = e.items().stream()
                    .filter(i -> i.role().isCore() && i.subcategory().texture() != null)
                    .map(i -> l.inline(i.subcategory()))
                    .distinct()
                    .toList();
            main = l.capitalize(l.t("explain.texture.mixed", l.join(names), textures));
        } else if (c.textures().size() == 1) {
            main = l.capitalize(l.t("explain.texture.single", textures));
        } else {
            main = l.capitalize(l.t("explain.texture.neutral"));
        }
        if (!c.mismatch().isEmpty()) {
            WardrobeItem formal = c.mismatch().get(0);
            WardrobeItem casual = c.mismatch().get(1);
            // Suggest replacing the piece furthest from the outfit's overall formality; on a tie the
            // smaller piece (usually the shoes) is the easier swap.
            double formalGap = Math.abs(formal.formality() - c.formality());
            double casualGap = Math.abs(casual.formality() - c.formality());
            WardrobeItem odd = Math.abs(formalGap - casualGap) < 0.3
                    ? (formal.role().visualArea() <= casual.role().visualArea() ? formal : casual)
                    : (formalGap > casualGap ? formal : casual);
            main = l.then(main, l.capitalize(l.t("explain.texture.mismatch", phrase(formal), phrase(casual),
                    l.inline(odd.role()))));
        } else if (!c.highLow().isEmpty() && c.pairing() != CompatibilityAnalysis.Pairing.SMART_MIX) {
            main = l.then(main, l.capitalize(l.t("explain.texture.highLow", phrase(c.highLow().get(0)),
                    phrase(c.highLow().get(1)))));
        }
        return main;
    }

    // ---- Kullanım Alanı (occasion) --------------------------------------------------------------

    String occasionText(OccasionAnalysis o) {
        String text = "";
        Occasion requested = o.requested();
        if (requested != null && o.scores().get(requested) < 0.6) {
            boolean tooCasual = o.formality() < requested.minFormality();
            text = l.capitalize(l.t(tooCasual ? "explain.occasion.tooCasual" : "explain.occasion.tooFormal",
                    l.inline(requested)));
        }
        List<Occasion> occasions = o.occasions();
        String main = occasions.size() >= 2
                ? l.t("explain.occasion.two", l.inline(occasions.get(0)), l.inline(occasions.get(1)))
                : l.t("explain.occasion.single." + o.primary().name());
        text = l.then(text, l.capitalize(main));
        if (!o.venues().isEmpty()) {
            String venues = l.join(o.venues().stream().map(l::inline).toList());
            text = l.then(text, l.capitalize(l.t("explain.occasion.venues", venues)));
        }
        return text;
    }

    // ---- Mevsim (season) ------------------------------------------------------------------------

    String seasonText(SeasonAnalysis s) {
        String season = l.inline(s.season());
        return switch (s.warmth()) {
            case TOO_LIGHT -> l.capitalize(l.t("explain.season.tooLight", season));
            case TOO_WARM -> l.capitalize(l.t("explain.season.tooWarm", season));
            case OK -> s.layer() == null ? null
                    : l.capitalize(l.t(s.season() == Season.WINTER ? "explain.season.layerWinter" : "explain.season.layer",
                            season, phrase(s.layer())));
        };
    }

    // ---- Başlık (title) -------------------------------------------------------------------------

    /** Fitting title ids (message keys {@code title.<id>}), preferred first; the preference varies per outfit. */
    static List<String> titleIds(OutfitEvaluation e) {
        int variant = Math.floorMod(e.key().hashCode(), 3);
        StylePreference style = e.style().style();
        Occasion primary = e.occasion().primary();
        if (primary == Occasion.EVENING && style != StylePreference.SPORT) {
            return rotate(variant, "cityEvening", "eveningChic", "nightElegance");
        }
        if (e.color().colorCase() == ColorAnalysis.ColorCase.TONAL && variant == 0) {
            List<String> tonal = new ArrayList<>();
            tonal.add("tonal");
            tonal.addAll(byStyle(e, style, primary, 1));
            return tonal;
        }
        return byStyle(e, style, primary, variant);
    }

    private static List<String> byStyle(OutfitEvaluation e, StylePreference style, Occasion primary, int variant) {
        return switch (style) {
            case CLASSIC -> primary == Occasion.OFFICE
                    ? rotate(variant, "officeChic", "timeless", "classicBalance")
                    : rotate(variant, "timeless", "classicBalance", "quietElegance");
            case SMART_CASUAL -> rotate(variant, "smartCasual", "relaxedChic", "polishedEase");
            case MINIMAL -> primary == Occasion.OFFICE
                    ? rotate(0, "minimalOffice", "cleanSharp")
                    : rotate(variant, "minimalBalance", "cleanSharp",
                            e.compatibility().formality() >= 3.2 ? "quietLuxury" : "simpleComfort");
            case BUSINESS -> rotate(variant, "meetingDay", "strongPresence", "businessChic");
            case STREETWEAR -> rotate(variant, "streetStyle", "cityRhythm", "relaxedCool");
            case SPORT -> rotate(variant, "activeDay", "freedomOfMovement", "sportyBalance");
            case CASUAL -> primary == Occasion.WEEKEND
                    ? rotate(variant, "weekendEasy", "sundayMood", "weekendEscape")
                    : rotate(variant, "everydayEase", "easyChic", "everydayOutfit");
        };
    }

    /** All options, starting with {@code variant} and wrapping around. */
    private static List<String> rotate(int variant, String... ids) {
        List<String> ordered = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            ordered.add(ids[(variant + i) % ids.length]);
        }
        return ordered;
    }

    private String phrase(WardrobeItem item) {
        return l.phrase(item.color(), item.subcategory());
    }

    private String colors(List<ColorName> colors) {
        return l.join(colors.stream().map(l::inline).toList());
    }
}
