package com.ryvenca.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ryvenca.image.MediaStorage;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MediaStorage storage;

    public WebConfig(MediaStorage storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // File names are random UUIDs and never change content, so they can be cached aggressively.
        registry.addResourceHandler("/media/**")
                .addResourceLocations(storage.root().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
