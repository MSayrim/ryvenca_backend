package com.ryvenca.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.DispatcherServlet;

import com.ryvenca.i18n.LanguageLocaleResolver;

@Configuration
public class I18nConfig {

    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    LocaleResolver localeResolver() {
        return new LanguageLocaleResolver();
    }
}
