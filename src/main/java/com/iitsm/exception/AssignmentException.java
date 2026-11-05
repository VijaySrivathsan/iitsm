package com.iitsm.exception;

/**
 * Base class for problems assigning an incident to a technician.
 */
public abstract class AssignmentException extends ItsmException {

    protected AssignmentException(String message) {
        super(message);
    }
}
