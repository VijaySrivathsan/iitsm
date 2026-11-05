package com.iitsm.exception;

/**
 * Base class for "the thing you asked for does not exist" errors (shown as HTTP 404).
 */
public abstract class NotFoundException extends ItsmException {

    protected NotFoundException(String message) {
        super(message);
    }
}
