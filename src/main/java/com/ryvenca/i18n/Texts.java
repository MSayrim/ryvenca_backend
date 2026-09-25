package com.ryvenca.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Entry point for localized text in the current request's language. */
@Component
public class Texts {

    private final MessageSource messages;

    public Texts(MessageSource messages) {
        this.messages = messages;
    }

    /** Localizer for the language of the current request (Accept-Language). */
    public Localizer current() {
        return of(Language.of(LocaleContextHolder.getLocale()));
    }

    public Localizer of(Language language) {
        return new Localizer(messages, language);
    }

    public Localizer of(Locale locale) {
        return of(Language.of(locale));
    }

    public String t(String key, Object... args) {
        return current().t(key, args);
    }
}
