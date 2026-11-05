package com.iitsm.support;

import com.iitsm.config.PasswordConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import java.time.Instant;

/**
 * Loads every business-layer component (no web, no security filters) on top of a @DataJpaTest,
 * with a {@link MutableClock} instead of the system clock.
 */
@TestConfiguration
@ComponentScan(basePackages = {
        "com.iitsm.domain", "com.iitsm.service", "com.iitsm.assignment", "com.iitsm.priority",
        "com.iitsm.sla", "com.iitsm.event", "com.iitsm.audit", "com.iitsm.notification",
        "com.iitsm.intelligence"},
        // Skip other tests' @TestConfiguration classes (e.g. a deliberately broken gateway) on the classpath.
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = TestConfiguration.class))
@Import(PasswordConfig.class)
public class ServiceLayerTestConfig {

    public static final Instant START = Instant.parse("2026-11-06T10:00:00Z");

    @Bean
    MutableClock clock() {
        return new MutableClock(START);
    }
}
