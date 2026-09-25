package com.ryvenca.i18n;

import org.springframework.context.support.ResourceBundleMessageSource;

/** The production message files, without a Spring context. */
public final class TestTexts {

    private static final ResourceBundleMessageSource SOURCE = new ResourceBundleMessageSource();

    static {
        SOURCE.setBasename("i18n/messages");
        SOURCE.setDefaultEncoding("UTF-8");
        SOURCE.setFallbackToSystemLocale(false);
    }

    private TestTexts() {
    }

    public static Localizer localizer(Language language) {
        return new Localizer(SOURCE, language);
    }
}
