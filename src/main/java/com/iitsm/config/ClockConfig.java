package com.iitsm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the system {@link Clock}. Every class that needs "now" asks this clock instead of calling
 * {@code Instant.now()} directly, so tests (and the demo) can use a fixed or fast-forwarded clock to
 * check SLA deadlines and escalation.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
