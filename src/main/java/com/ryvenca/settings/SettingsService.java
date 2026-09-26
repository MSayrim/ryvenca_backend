package com.ryvenca.settings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.auth.AuthService;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.firebase.FirebaseAuthGateway;
import com.ryvenca.settings.SettingsDtos.AdminSettings;
import com.ryvenca.settings.SettingsDtos.AuthConfig;
import com.ryvenca.settings.SettingsDtos.FirebaseWeb;
import com.ryvenca.settings.SettingsDtos.Links;
import com.ryvenca.settings.SettingsDtos.Maintenance;
import com.ryvenca.settings.SettingsDtos.MinVersion;
import com.ryvenca.settings.SettingsDtos.Providers;
import com.ryvenca.settings.SettingsDtos.PublicConfig;
import com.ryvenca.settings.SettingsDtos.SettingsUpdate;
import com.ryvenca.settings.SettingsDtos.Status;

/** Admin-managed configuration stored as key/value rows, with typed access and validation. */
@Service
public class SettingsService {

    private static final Pattern URL = Pattern.compile("^https?://[^\\s/$.?#][^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern VERSION = Pattern.compile("^\\d{1,4}(\\.\\d{1,4}){0,2}$");
    private static final int MAX_TEXT = 1000;

    private final AppSettingRepository settings;
    private final FirebaseAuthGateway firebase;
    private final AuthService auth;

    public SettingsService(AppSettingRepository settings, FirebaseAuthGateway firebase, AuthService auth) {
        this.settings = settings;
        this.firebase = firebase;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public PublicConfig publicConfig() {
        Map<String, String> v = values();
        FirebaseWeb web = firebaseWeb(v);
        Maintenance maintenance = maintenance(v);
        return new PublicConfig(new AuthConfig(firebase.available(), auth.localEnabled(), providers(v)),
                web.complete() ? web : null, links(v), maintenance, minVersion(v));
    }

    @Transactional(readOnly = true)
    public AdminSettings adminSettings() {
        Map<String, String> v = values();
        return new AdminSettings(providers(v), firebaseWeb(v), links(v), maintenance(v), minVersion(v),
                new Status(firebase.available(), firebase.projectId().orElse(null), auth.localEnabled()));
    }

    @Transactional
    public AdminSettings update(SettingsUpdate update, long adminId) {
        Map<String, String> changes = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        if (update.providers() != null) {
            bool(changes, "providers.apple", update.providers().apple());
            bool(changes, "providers.google", update.providers().google());
            bool(changes, "providers.email", update.providers().email());
        }
        FirebaseWeb w = update.firebaseWeb();
        if (w != null) {
            text(changes, errors, "firebaseWeb.apiKey", w.apiKey(), null);
            text(changes, errors, "firebaseWeb.authDomain", w.authDomain(), null);
            text(changes, errors, "firebaseWeb.projectId", w.projectId(), null);
            text(changes, errors, "firebaseWeb.appId", w.appId(), null);
            text(changes, errors, "firebaseWeb.messagingSenderId", w.messagingSenderId(), null);
            text(changes, errors, "firebaseWeb.storageBucket", w.storageBucket(), null);
        }
        Links l = update.links();
        if (l != null) {
            text(changes, errors, "links.privacyPolicy", l.privacyPolicy(), URL);
            text(changes, errors, "links.terms", l.terms(), URL);
            text(changes, errors, "links.support", l.support(), URL);
            text(changes, errors, "links.supportEmail", l.supportEmail(), EMAIL);
            text(changes, errors, "links.accountDeletion", l.accountDeletion(), URL);
            text(changes, errors, "links.appStore", l.appStore(), URL);
            text(changes, errors, "links.playStore", l.playStore(), URL);
        }
        if (update.maintenance() != null) {
            bool(changes, "maintenance.enabled", update.maintenance().enabled());
            text(changes, errors, "maintenance.message", update.maintenance().message(), null);
        }
        if (update.minVersion() != null) {
            text(changes, errors, "minVersion.ios", update.minVersion().ios(), VERSION);
            text(changes, errors, "minVersion.android", update.minVersion().android(), VERSION);
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors.values().iterator().next(), errors);
        }
        changes.forEach((key, value) -> {
            AppSetting setting = settings.findById(key).orElseGet(() -> new AppSetting(key));
            setting.update(value, adminId);
            settings.save(setting);
        });
        return adminSettings();
    }

    private Map<String, String> values() {
        Map<String, String> map = new HashMap<>();
        settings.findAll().forEach(s -> {
            if (s.getValue() != null && !s.getValue().isEmpty()) {
                map.put(s.getKey(), s.getValue());
            }
        });
        return map;
    }

    private static Providers providers(Map<String, String> v) {
        return new Providers(flag(v, "providers.apple", true), flag(v, "providers.google", true),
                flag(v, "providers.email", true));
    }

    private static FirebaseWeb firebaseWeb(Map<String, String> v) {
        return new FirebaseWeb(v.get("firebaseWeb.apiKey"), v.get("firebaseWeb.authDomain"),
                v.get("firebaseWeb.projectId"), v.get("firebaseWeb.appId"), v.get("firebaseWeb.messagingSenderId"),
                v.get("firebaseWeb.storageBucket"));
    }

    private static Links links(Map<String, String> v) {
        return new Links(v.get("links.privacyPolicy"), v.get("links.terms"), v.get("links.support"),
                v.get("links.supportEmail"), v.get("links.accountDeletion"), v.get("links.appStore"),
                v.get("links.playStore"));
    }

    private static Maintenance maintenance(Map<String, String> v) {
        return new Maintenance(flag(v, "maintenance.enabled", false), v.get("maintenance.message"));
    }

    private static MinVersion minVersion(Map<String, String> v) {
        return new MinVersion(v.get("minVersion.ios"), v.get("minVersion.android"));
    }

    private static boolean flag(Map<String, String> v, String key, boolean fallback) {
        String value = v.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static void bool(Map<String, String> changes, String key, Boolean value) {
        if (value != null) {
            changes.put(key, value.toString());
        }
    }

    private static void text(Map<String, String> changes, Map<String, String> errors, String key, String value,
                             Pattern pattern) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TEXT) {
            errors.put(key, "validation.setting.tooLong");
        } else if (!trimmed.isEmpty() && pattern != null && !pattern.matcher(trimmed).matches()) {
            errors.put(key, pattern == URL ? "validation.setting.url"
                    : pattern == EMAIL ? "validation.email.invalid" : "validation.setting.version");
        } else {
            changes.put(key, trimmed);
        }
    }
}
