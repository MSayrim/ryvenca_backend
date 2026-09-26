package com.ryvenca.settings;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One admin-managed setting. */
@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    @Column(name = "setting_key")
    private String key;

    @Column(name = "setting_value")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected AppSetting() {
    }

    public AppSetting(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void update(String value, Long by) {
        this.value = value;
        this.updatedBy = by;
        this.updatedAt = Instant.now();
    }
}
