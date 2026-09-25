package com.ryvenca.image;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ryvenca.config.RyvencaProperties;

/** Builds absolute URLs for media files, honoring X-Forwarded-* headers or a configured public base URL. */
@Component
public class MediaUrls {

    private final String configuredBase;

    public MediaUrls(RyvencaProperties properties) {
        String base = properties.storage().publicBaseUrl();
        this.configuredBase = base == null || base.isBlank() ? null : base.replaceAll("/+$", "");
    }

    public String url(String fileName) {
        return base() + "/media/" + fileName;
    }

    private String base() {
        if (configuredBase != null) {
            return configuredBase;
        }
        if (RequestContextHolder.getRequestAttributes() != null) {
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }
        return "";
    }
}
