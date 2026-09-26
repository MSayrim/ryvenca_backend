package com.ryvenca.settings;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryvenca.settings.SettingsDtos.PublicConfig;

/** Public runtime configuration for the clients (links, sign-in providers, maintenance …). */
@RestController
public class ConfigController {

    private final SettingsService settings;

    public ConfigController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/api/config")
    public ResponseEntity<PublicConfig> config() {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(settings.publicConfig());
    }
}
