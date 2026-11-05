package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * The technician is investigating. They can ask the reporter for information or resolve it.
 */
final class InProgressState extends AbstractIncidentState {

    InProgressState() {
        super(IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentOperation.REQUEST_INFO, IncidentOperation.RESOLVE, IncidentOperation.ASSIGN));
    }

    @Override
    public IncidentStatus requestInfo() {
        return IncidentStatus.PENDING_INFO;
    }

    @Override
    public IncidentStatus resolve() {
        return IncidentStatus.RESOLVED;
    }

    @Override
    public IncidentStatus assign() {
        return IncidentStatus.ASSIGNED;
    }
}
