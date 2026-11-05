package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * Waiting for the reporter to answer an information request.
 */
final class PendingInfoState extends AbstractIncidentState {

    PendingInfoState() {
        super(IncidentStatus.PENDING_INFO, EnumSet.of(IncidentOperation.PROVIDE_INFO));
    }

    @Override
    public IncidentStatus provideInfo() {
        return IncidentStatus.IN_PROGRESS;
    }
}
