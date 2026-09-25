package com.ryvenca.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The 16 UI languages RYVENCA supports: Turkish (product default) + the 15 most spoken languages. */
public enum Language {
    TR("tr", "Türkçe", false),
    EN("en", "English", false),
    ZH("zh", "简体中文", false),
    HI("hi", "हिन्दी", false),
    ES("es", "Español", false),
    AR("ar", "العربية", true),
    FR("fr", "Français", false),
    BN("bn", "বাংলা", false),
    PT("pt", "Português", false),
    RU("ru", "Русский", false),
    ID("id", "Bahasa Indonesia", false),
    UR("ur", "اردو", true),
    DE("de", "Deutsch", false),
    JA("ja", "日本語", false),
    VI("vi", "Tiếng Việt", false),
    KO("ko", "한국어", false);

    public static final Language DEFAULT = TR;
    public static final Language FALLBACK = EN;

    private final String code;
    private final String nativeName;
    private final boolean rtl;
    private final Locale locale;

    Language(String code, String nativeName, boolean rtl) {
        this.code = code;
        this.nativeName = nativeName;
        this.rtl = rtl;
        this.locale = Locale.forLanguageTag(code);
    }

    public String code() {
        return code;
    }

    public String nativeName() {
        return nativeName;
    }

    public boolean rtl() {
        return rtl;
    }

    public Locale locale() {
        return locale;
    }

    /** Matches a language tag on its primary subtag ("zh-CN" → ZH, "pt_BR" → PT). */
    public static Optional<Language> fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return Optional.empty();
        }
        String primary = tag.trim().replace('_', '-').split("-")[0].toLowerCase(Locale.ROOT);
        // Legacy ISO codes Java may still produce.
        if (primary.equals("in")) {
            primary = "id";
        }
        String code = primary;
        return Arrays.stream(values()).filter(l -> l.code.equals(code)).findFirst();
    }

    public static Language of(Locale locale) {
        return fromTag(locale.toLanguageTag()).orElse(FALLBACK);
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(Language::code).toList();
    }
}
