package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * A fix has been provided. The reporter verifies it, or reopens the incident if it is not fixed.
 */
final class ResolvedState extends AbstractIncidentState {

    ResolvedState() {
        super(IncidentStatus.RESOLVED, EnumSet.of(IncidentOperation.VERIFY, IncidentOperation.REOPEN));
    }

    @Override
    public IncidentStatus verify() {
        return IncidentStatus.VERIFIED;
    }

    @Override
    public IncidentStatus reopen() {
        return IncidentStatus.IN_PROGRESS;
    }
}
