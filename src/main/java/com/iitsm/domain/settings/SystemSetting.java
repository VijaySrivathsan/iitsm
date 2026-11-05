package com.iitsm.domain.settings;

import com.iitsm.domain.Checks;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A setting an administrator can change while the system runs, stored as a key/value pair
 * (e.g. {@code assignment.strategy = skill}). Keeping it in the database means the choice survives
 * a restart. Identified by its key, so it does not extend BaseEntity.
 */
@Entity
@Table(name = "system_setting")
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 80)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String value;

    protected SystemSetting() {
    }

    public SystemSetting(String key, String value) {
        this.key = Checks.requireText(key, "Setting key");
        this.value = Checks.requireText(value, "Setting value");
    }

    public void changeValue(String value) {
        this.value = Checks.requireText(value, "Setting value");
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
