package com.ryvenca.color;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * RYVENCA's own color combination dataset (resources/palettes.json). Inspired by the idea of
 * curated combination dictionaries, but every palette here is our own.
 */
@Component
public class PaletteLibrary {

    private record PaletteJson(String id, String name, List<String> colors) {
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
        try (InputStream in = new ClassPathResource("palettes.json").getInputStream()) {
            PaletteJson[] raw = JsonMapper.builder().build().readValue(in, PaletteJson[].class);
            return java.util.Arrays.stream(raw)
                    .map(p -> new ColorPalette(p.id(), p.name(), p.colors(),
                            p.colors().stream().map(ColorScience::hexToLab).toList()))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("palettes.json could not be loaded", e);
        }
    }
}
