package com.ryvenca.catalog;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.color.ColorName;

/** Labels and display order for every enum, so the clients never hard-code Turkish copy. */
@RestController
public class MetaController {

    public record Option(String code, String label) {
    }

    public record StyleOption(String code, String label, String description) {
    }

    public record ColorOption(String code, String label, String hex) {
    }

    public record CategoryOption(String code, String label, String pluralLabel, List<Option> subcategories) {
    }

    public record MetaResponse(List<Option> wardrobeTypes, List<StyleOption> styles, List<CategoryOption> categories,
                               List<ColorOption> colors, List<Option> seasons, List<Option> occasions) {
    }

    private static final MetaResponse META = new MetaResponse(
            options(WardrobeType.values()),
            Arrays.stream(StylePreference.values())
                    .map(s -> new StyleOption(s.name(), s.label(), s.description())).toList(),
            Arrays.stream(Category.values())
                    .map(c -> new CategoryOption(c.name(), c.label(), c.pluralLabel(),
                            options(Subcategory.of(c).toArray(Labeled[]::new))))
                    .toList(),
            Arrays.stream(ColorName.values()).map(c -> new ColorOption(c.name(), c.label(), c.hex())).toList(),
            options(Season.values()),
            options(Occasion.values()));

    @GetMapping("/api/meta")
    public MetaResponse meta() {
        return META;
    }

    private static List<Option> options(Labeled[] values) {
        return Arrays.stream(values).map(v -> new Option(v.name(), v.label())).toList();
    }
}
