package com.ryvenca.catalog;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.color.ColorName;
import com.ryvenca.i18n.Language;
import com.ryvenca.i18n.Localizer;
import com.ryvenca.i18n.Texts;

/** Localized labels and display order for every enum, so the clients never hard-code copy. */
@RestController
public class MetaController {

    public record Option(String code, String label) {
    }

    public record LanguageOption(String code, String label, boolean rtl) {
    }

    public record StyleOption(String code, String label, String description) {
    }

    public record ColorOption(String code, String label, String hex) {
    }

    public record CategoryOption(String code, String label, String pluralLabel, List<Option> subcategories) {
    }

    public record MetaResponse(List<LanguageOption> languages, List<Option> wardrobeTypes, List<StyleOption> styles,
                               List<CategoryOption> categories, List<ColorOption> colors, List<Option> seasons,
                               List<Option> occasions) {
    }

    private final Texts texts;

    public MetaController(Texts texts) {
        this.texts = texts;
    }

    @GetMapping("/api/meta")
    public MetaResponse meta() {
        Localizer l = texts.current();
        return new MetaResponse(
                Arrays.stream(Language.values())
                        .map(lang -> new LanguageOption(lang.code(), lang.nativeName(), lang.rtl())).toList(),
                options(l, WardrobeType.values()),
                Arrays.stream(StylePreference.values())
                        .map(s -> new StyleOption(s.name(), l.label(s), l.t(s.descriptionKey()))).toList(),
                Arrays.stream(Category.values())
                        .map(c -> new CategoryOption(c.name(), l.label(c), l.t(c.pluralLabelKey()),
                                options(l, Subcategory.of(c).toArray(Labeled[]::new))))
                        .toList(),
                Arrays.stream(ColorName.values()).map(c -> new ColorOption(c.name(), l.label(c), c.hex())).toList(),
                options(l, Season.values()),
                options(l, Occasion.values()));
    }

    private static List<Option> options(Localizer l, Labeled[] values) {
        return Arrays.stream(values).map(v -> new Option(v.name(), l.label(v))).toList();
    }
}
