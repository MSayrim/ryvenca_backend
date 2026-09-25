package com.ryvenca.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolves the request language from {@code Accept-Language}: the first supported language in
 * preference order wins, no header means Turkish (the product default), anything unsupported English.
 */
public class LanguageLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return resolve(request.getHeader("Accept-Language")).locale();
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("The language is chosen by the client via Accept-Language");
    }

    public static Language resolve(String header) {
        if (header == null || header.isBlank()) {
            return Language.DEFAULT;
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(header);
        } catch (IllegalArgumentException e) {
            return Language.fromTag(header).orElse(Language.FALLBACK);
        }
        for (Locale.LanguageRange range : ranges) {
            Optional<Language> language = Language.fromTag(range.getRange());
            if (language.isPresent() && range.getWeight() > 0) {
                return language.get();
            }
        }
        return Language.FALLBACK;
    }
}
