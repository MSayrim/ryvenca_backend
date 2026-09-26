package com.ryvenca.color;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.json.JsonMapper;

/**
 * An immutable set of curated color combinations. The built-in dataset (resources/palettes.json) is
 * RYVENCA's own; at runtime the enabled palettes come from the database (see {@code PaletteService}),
 * where admins can edit them.
 */
public class PaletteLibrary {

    /** One entry of the built-in dataset. */
    public record PaletteJson(String id, List<String> colors) {
    }

    private final List<ColorPalette> palettes;

    public PaletteLibrary() {
        this(load());
    }

    public PaletteLibrary(List<ColorPalette> palettes) {
        this.palettes = List.copyOf(palettes);
    }

    public List<ColorPalette> palettes() {
        return palettes;
    }

    public int size() {
        return palettes.size();
    }

    public ColorPalette get(int index) {
        return palettes.get(index);
    }

    private static List<ColorPalette> load() {
        return builtIn().stream().map(p -> ColorPalette.of(p.id(), p.colors())).toList();
    }

    /** The built-in dataset shipped with the server. */
    public static List<PaletteJson> builtIn() {
        try (InputStream in = new ClassPathResource("palettes.json").getInputStream()) {
            return List.of(JsonMapper.builder().build().readValue(in, PaletteJson[].class));
        } catch (IOException e) {
            throw new IllegalStateException("palettes.json could not be loaded", e);
        }
    }
}
