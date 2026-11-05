package com.iitsm.exception;

/**
 * Thrown when a logged-in user tries something their role or relationship to the incident does not allow (HTTP 403).
 */
public class UnauthorizedOperationException extends ItsmException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
