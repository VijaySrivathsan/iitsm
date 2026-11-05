package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * Category, priority and SLA are set; waiting for a technician. An administrator may reclassify.
 */
final class ClassifiedState extends AbstractIncidentState {

    ClassifiedState() {
        super(IncidentStatus.CLASSIFIED, EnumSet.of(IncidentOperation.CLASSIFY, IncidentOperation.ASSIGN));
    }

    @Override
    public IncidentStatus classify() {
        return IncidentStatus.CLASSIFIED;
    }

    @Override
    public IncidentStatus assign() {
        return IncidentStatus.ASSIGNED;
    }
}
