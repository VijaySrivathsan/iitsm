package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * A technician has been given the incident but has not acknowledged it yet (escalation watches this state).
 */
final class AssignedState extends AbstractIncidentState {

    AssignedState() {
        super(IncidentStatus.ASSIGNED, EnumSet.of(IncidentOperation.ACKNOWLEDGE, IncidentOperation.ASSIGN));
    }

    @Override
    public IncidentStatus acknowledge() {
        return IncidentStatus.ACKNOWLEDGED;
    }

    @Override
    public IncidentStatus assign() {
        return IncidentStatus.ASSIGNED;
    }
}
