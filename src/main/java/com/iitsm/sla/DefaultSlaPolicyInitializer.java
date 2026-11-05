package com.iitsm.sla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On start-up, makes sure the default SLA policies (P1-P4) exist, in every profile. Without them no
 * incident could be classified.
 */
@Component
public class DefaultSlaPolicyInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultSlaPolicyInitializer.class);

    private final SlaService slaService;

    public DefaultSlaPolicyInitializer(SlaService slaService) {
        this.slaService = slaService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = slaService.ensureDefaultPolicies();
        if (created > 0) {
            log.info("Created {} default SLA policies", created);
        }
    }
}
