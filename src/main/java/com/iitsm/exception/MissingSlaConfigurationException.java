package com.iitsm.exception;

import com.iitsm.domain.incident.Priority;

/**
 * Thrown when neither a service-specific nor a default SLA policy exists for a priority.
 */
public class MissingSlaConfigurationException extends ItsmException {

    public MissingSlaConfigurationException(String serviceName, Priority priority) {
        super("No SLA policy configured for " + priority + " (service " + serviceName
                + ") and no default policy for " + priority);
    }
}
