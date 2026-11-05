package com.iitsm.exception;

/**
 * An external system (email server, SMS provider, ...) failed or could not be reached.
 * Notification channels catch this so a failed message never blocks an incident update.
 */
public class ExternalServiceException extends ItsmException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
