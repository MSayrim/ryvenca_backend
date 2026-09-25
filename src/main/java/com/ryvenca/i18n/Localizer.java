package com.ryvenca.i18n;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import com.ryvenca.catalog.Labeled;

/**
 * Renders localized text for one language. Messages are read raw (no {@link java.text.MessageFormat},
 * so apostrophes need no escaping) and {@code {0}}, {@code {1}} … placeholders are replaced here.
 *
 * <p>Per-language grammar lives in the message files too: how a color and a garment form a phrase
 * ({@code grammar.phrase}), how lists are joined, how sentences are joined and whether labels are
 * lowercased inside sentences.
 */
public final class Localizer {

    private final MessageSource messages;
    private final Language language;

    public Localizer(MessageSource messages, Language language) {
        this.messages = messages;
        this.language = language;
    }

    public Language language() {
        return language;
    }

    public Locale locale() {
        return language.locale();
    }

    public String t(String key, Object... args) {
        String raw;
        try {
            raw = messages.getMessage(key, null, language.locale());
        } catch (NoSuchMessageException e) {
            raw = messages.getMessage(key, null, key, Language.FALLBACK.locale());
        }
        return format(raw, args);
    }

    public String label(Labeled value) {
        return t(value.labelKey());
    }

    /** Label as used inside a sentence (lowercased in languages that do so, e.g. "beige" not "Beige"). */
    public String inline(Labeled value) {
        return inline(label(value));
    }

    public String inline(String text) {
        return Boolean.parseBoolean(t("grammar.lowercase")) ? text.toLowerCase(locale()) : text;
    }

    /** "beige blazer" / "blazer beige" / "ベージュのブレザー" … */
    public String phrase(Labeled color, Labeled garment) {
        return t("grammar.phrase", inline(color), inline(garment));
    }

    public String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        int first = text.codePointAt(0);
        return new String(Character.toChars(Character.toTitleCase(first))) + text.substring(Character.charCount(first));
    }

    /** "a", "a and b", "a, b and c" with the language's own separators. */
    public String join(List<String> parts) {
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        String head = parts.getFirst();
        for (int i = 1; i < parts.size() - 1; i++) {
            head = t("grammar.list.middle", head, parts.get(i));
        }
        return t("grammar.list.last", head, parts.getLast());
    }

    /** Appends a sentence ("A. B." in most languages, "A。B。" in Chinese/Japanese). */
    public String then(String first, String second) {
        if (first == null || first.isEmpty()) {
            return second;
        }
        return t("grammar.sentences", first, second);
    }

    static String format(String pattern, Object... args) {
        if (args == null || args.length == 0 || pattern.indexOf('{') < 0) {
            return pattern;
        }
        StringBuilder out = new StringBuilder(pattern.length() + 32);
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') {
                int end = pattern.indexOf('}', i);
                if (end > i + 1) {
                    String index = pattern.substring(i + 1, end);
                    if (index.chars().allMatch(Character::isDigit)) {
                        int n = Integer.parseInt(index);
                        if (n < args.length) {
                            out.append(args[n]);
                            i = end;
                            continue;
                        }
                    }
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
