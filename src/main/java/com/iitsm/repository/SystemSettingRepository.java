package com.iitsm.repository;

import com.iitsm.domain.settings.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for runtime settings, keyed by setting name.
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
