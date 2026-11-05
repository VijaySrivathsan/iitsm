package com.iitsm.exception;

/**
 * Thrown when a value violates a domain rule (blank name, out-of-range criticality, bad password, ...).
 */
public class InvalidInputException extends ItsmException {

    public InvalidInputException(String message) {
        super(message);
    }
}
