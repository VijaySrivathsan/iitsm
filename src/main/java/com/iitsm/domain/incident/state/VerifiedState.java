package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * The reporter confirmed the fix. Ready to be closed.
 */
final class VerifiedState extends AbstractIncidentState {

    VerifiedState() {
        super(IncidentStatus.VERIFIED, EnumSet.of(IncidentOperation.CLOSE));
    }

    @Override
    public IncidentStatus close() {
        return IncidentStatus.CLOSED;
    }
}
