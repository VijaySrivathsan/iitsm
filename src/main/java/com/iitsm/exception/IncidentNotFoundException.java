package com.iitsm.exception;

/**
 * Thrown when no incident has the requested number.
 */
public class IncidentNotFoundException extends NotFoundException {

    public IncidentNotFoundException(String incidentNumber) {
        super("Incident " + incidentNumber + " was not found");
    }
}
