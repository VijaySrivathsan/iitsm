package com.iitsm.exception;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;

/**
 * Thrown by the State pattern when an operation is not allowed in the incident's current status,
 * e.g. closing a NEW incident. Keeps the status and operation so the UI can explain the problem.
 */
public class InvalidStateTransitionException extends ItsmException {

    private final IncidentStatus currentStatus;
    private final IncidentOperation operation;

    public InvalidStateTransitionException(IncidentStatus currentStatus, IncidentOperation operation) {
        super("Cannot " + operation.label().toLowerCase() + " an incident that is " + currentStatus);
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public IncidentOperation getOperation() {
        return operation;
    }
}
