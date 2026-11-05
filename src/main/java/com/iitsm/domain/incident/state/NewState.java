package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumSet;

/**
 * Just reported. The only way forward is classification (category, priority, SLA).
 */
final class NewState extends AbstractIncidentState {

    NewState() {
        super(IncidentStatus.NEW, EnumSet.of(IncidentOperation.CLASSIFY));
    }

    @Override
    public IncidentStatus classify() {
        return IncidentStatus.CLASSIFIED;
    }
}
