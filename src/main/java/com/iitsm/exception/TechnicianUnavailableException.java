package com.iitsm.exception;

/**
 * Thrown when the chosen technician is inactive or marked unavailable, or no technician can take the incident.
 */
public class TechnicianUnavailableException extends AssignmentException {

    public TechnicianUnavailableException(String message) {
        super(message);
    }
}
