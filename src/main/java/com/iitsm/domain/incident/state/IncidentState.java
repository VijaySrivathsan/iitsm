package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

import java.util.Set;

/**
 * State pattern: one implementation per lifecycle status. Each operation either returns the status
 * the incident moves to, or throws {@link com.iitsm.exception.InvalidStateTransitionException} if it
 * is not allowed in this state.
 *
 * <p>States only decide <i>whether</i> a transition is legal and <i>where</i> it leads. The
 * {@link com.iitsm.domain.incident.Incident} applies the change to its own fields, and the service
 * layer checks <i>who</i> may do it.
 */
public interface IncidentState {

    IncidentStatus status();

    /** Operations allowed in this state (used e.g. to show only valid buttons in the UI). */
    Set<IncidentOperation> allowedOperations();

    default boolean allows(IncidentOperation operation) {
        return allowedOperations().contains(operation);
    }

    IncidentStatus classify();

    IncidentStatus assign();

    IncidentStatus acknowledge();

    IncidentStatus startWork();

    IncidentStatus requestInfo();

    IncidentStatus provideInfo();

    IncidentStatus resolve();

    IncidentStatus verify();

    IncidentStatus reopen();

    IncidentStatus close();
}
