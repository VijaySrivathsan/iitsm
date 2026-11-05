package com.iitsm.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on background jobs (the SLA monitor). Disabled in tests with {@code itsm.scheduling.enabled=false},
 * where the monitor is run directly with a controllable clock.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "itsm.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
