package com.ryvenca.palette;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.color.ColorPalette;
import com.ryvenca.color.PaletteLibrary;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.i18n.Language;
import com.ryvenca.i18n.Localizer;
import com.ryvenca.i18n.Texts;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Owns the palettes the outfit engine uses: seeds the built-in dataset into the database, keeps an
 * in-memory snapshot of the enabled ones and lets admins edit them.
 */
@Service
public class PaletteService {

    private static final Logger log = LoggerFactory.getLogger(PaletteService.class);
    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, String>> NAMES = new TypeReference<>() {
    };

    public record PaletteDto(String id, List<String> colors, Map<String, String> names, boolean enabled,
                             boolean builtIn) {
    }

    public record PaletteRequest(List<String> colors, Map<String, String> names, Boolean enabled) {
    }

    private final PaletteRepository palettes;
    private final Texts texts;
    private volatile PaletteLibrary library;
    private volatile Map<String, Map<String, String>> customNames = Map.of();
    private volatile Map<String, Boolean> builtIn = Map.of();

    public PaletteService(PaletteRepository palettes, Texts texts) {
        this.palettes = palettes;
        this.texts = texts;
        this.library = new PaletteLibrary();
    }

    /** Seeds new built-in palettes (existing rows, including admin edits, are kept) and loads the snapshot. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAndLoad() {
        Map<String, Palette> existing = new LinkedHashMap<>();
        palettes.findAll().forEach(p -> existing.put(p.getId(), p));
        int order = 0;
        int added = 0;
        for (PaletteLibrary.PaletteJson p : PaletteLibrary.builtIn()) {
            if (!existing.containsKey(p.id())) {
                palettes.save(new Palette(p.id(), p.colors(), true, order));
                added++;
            }
            order++;
        }
        if (added > 0) {
            log.info("Seeded {} built-in palettes", added);
        }
        reload();
    }

    /** The enabled palettes, as used by the outfit engine. */
    public PaletteLibrary library() {
        return library;
    }

    /** Display name in the given language: admin-entered name, else the translated built-in name, else English. */
    public String displayName(String id, Localizer l) {
        Map<String, String> names = customNames.getOrDefault(id, Map.of());
        String own = names.get(l.language().code());
        if (own != null) {
            return own;
        }
        if (builtIn.getOrDefault(id, false)) {
            return l.t("palette." + id);
        }
        return names.getOrDefault(Language.EN.code(), id);
    }

    @Transactional(readOnly = true)
    public List<PaletteDto> list() {
        return palettes.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public PaletteDto create(PaletteRequest request) {
        List<String> colors = validColors(request.colors());
        Map<String, String> names = validNames(request.names(), false, null);
        Palette palette = new Palette(nextId(), colors, false, nextOrder());
        palette.setNamesJson(json(names));
        palette.setEnabled(request.enabled() == null || request.enabled());
        palettes.save(palette);
        reload();
        return toDto(palette);
    }

    @Transactional
    public PaletteDto update(String id, PaletteRequest request) {
        Palette palette = palettes.findById(id).orElseThrow(() -> ApiException.notFound("error.palette.notFound"));
        if (request.colors() != null) {
            palette.setColors(validColors(request.colors()));
        }
        if (request.names() != null) {
            palette.setNamesJson(json(validNames(request.names(), palette.isBuiltIn(), id)));
        }
        if (request.enabled() != null) {
            palette.setEnabled(request.enabled());
        }
        palettes.save(palette);
        reload();
        return toDto(palette);
    }

    @Transactional
    public void delete(String id) {
        Palette palette = palettes.findById(id).orElseThrow(() -> ApiException.notFound("error.palette.notFound"));
        if (palette.isBuiltIn()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "error.palette.builtInDelete");
        }
        palettes.delete(palette);
        palettes.flush();
        reload();
    }

    @Transactional(readOnly = true)
    public synchronized void reload() {
        List<ColorPalette> enabled = new ArrayList<>();
        Map<String, Map<String, String>> names = new LinkedHashMap<>();
        Map<String, Boolean> builtIns = new LinkedHashMap<>();
        for (Palette p : palettes.findAllByOrderBySortOrderAscIdAsc()) {
            names.put(p.getId(), parse(p.getNamesJson()));
            builtIns.put(p.getId(), p.isBuiltIn());
            if (p.isEnabled() && p.getColors().size() >= 2) {
                enabled.add(ColorPalette.of(p.getId(), p.getColors()));
            }
        }
        this.customNames = Map.copyOf(names);
        this.builtIn = Map.copyOf(builtIns);
        this.library = new PaletteLibrary(enabled);
    }

    private PaletteDto toDto(Palette p) {
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, String> own = parse(p.getNamesJson());
        for (Language language : Language.values()) {
            String value = own.get(language.code());
            if (value == null && p.isBuiltIn()) {
                value = texts.of(language).t("palette." + p.getId());
            }
            if (value != null) {
                names.put(language.code(), value);
            }
        }
        return new PaletteDto(p.getId(), p.getColors(), names, p.isEnabled(), p.isBuiltIn());
    }

    private static List<String> validColors(List<String> colors) {
        if (colors == null || colors.size() < 2 || colors.size() > 5) {
            throw ApiException.invalidField("colors", "validation.palette.colors");
        }
        List<String> result = new ArrayList<>();
        for (String c : colors) {
            if (c == null || !HEX.matcher(c.trim()).matches()) {
                throw ApiException.invalidField("colors", "validation.palette.colors");
            }
            result.add(c.trim().toUpperCase(java.util.Locale.ROOT));
        }
        return result;
    }

    /** Keeps names for supported languages; built-in names equal to the shipped translation are not stored. */
    private Map<String, String> validNames(Map<String, String> names, boolean builtIn, String id) {
        Map<String, String> result = new LinkedHashMap<>();
        if (names != null) {
            for (Language language : Language.values()) {
                String value = names.get(language.code());
                if (value == null || value.isBlank()) {
                    continue;
                }
                String trimmed = value.trim();
                if (trimmed.length() > 60) {
                    throw ApiException.invalidField("names", "validation.palette.nameSize");
                }
                if (builtIn && trimmed.equals(texts.of(language).t("palette." + id))) {
                    continue;
                }
                result.put(language.code(), trimmed);
            }
        }
        if (!builtIn && !result.containsKey(Language.EN.code())) {
            throw ApiException.invalidField("names", "validation.palette.nameRequired");
        }
        return result;
    }

    private String nextId() {
        int max = palettes.findAll().stream().map(Palette::getId).filter(i -> i.matches("P\\d+"))
                .mapToInt(i -> Integer.parseInt(i.substring(1))).max().orElse(0);
        return String.format("P%03d", max + 1);
    }

    private int nextOrder() {
        return palettes.findAll().stream().mapToInt(Palette::getSortOrder).max().orElse(0) + 1;
    }

    private static Map<String, String> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return JSON.readValue(json, NAMES);
    }

    private static String json(Map<String, String> names) {
        return JSON.writeValueAsString(names);
    }
}
