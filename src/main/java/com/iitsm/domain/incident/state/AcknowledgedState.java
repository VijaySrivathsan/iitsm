package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * The technician has accepted the incident (the SLA response time is met here).
 */
final class AcknowledgedState extends AbstractIncidentState {

    AcknowledgedState() {
        super(IncidentStatus.ACKNOWLEDGED, EnumSet.of(IncidentOperation.START_WORK, IncidentOperation.ASSIGN));
    }

    @Override
    public IncidentStatus startWork() {
        return IncidentStatus.IN_PROGRESS;
    }

    @Override
    public IncidentStatus assign() {
        return IncidentStatus.ASSIGNED;
    }
}
