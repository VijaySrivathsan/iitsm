package com.iitsm.sla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job: runs the SLA checks on a fixed delay ({@code itsm.sla.monitor-interval}, default
 * one minute). Scheduling is switched on by SchedulingConfig.
 */
@Component
public class SlaMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitor.class);

    private final SlaMonitorService service;

    public SlaMonitor(SlaMonitorService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${itsm.sla.monitor-interval:PT60S}", initialDelayString = "PT10S")
    public void run() {
        MonitorResult result = service.runChecks();
        if (result.breaches() + result.warnings() + result.escalations() > 0) {
            log.info("SLA monitor: {} breach(es), {} warning(s), {} escalation(s)",
                    result.breaches(), result.warnings(), result.escalations());
        }
    }
}
