package com.iitsm.exception;

/**
 * Root of the I-ITSM exception hierarchy. All business errors extend this, so one handler can
 * turn any of them into a friendly message.
 */
public abstract class ItsmException extends RuntimeException {

    protected ItsmException(String message) {
        super(message);
    }

    protected ItsmException(String message, Throwable cause) {
        super(message, cause);
    }
}
