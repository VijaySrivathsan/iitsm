package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * Final state. Every operation is rejected.
 */
final class ClosedState extends AbstractIncidentState {

    ClosedState() {
        super(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentOperation.class));
    }
}
