package com.iitsm.exception;

/**
 * Thrown when an IT service id or name does not exist.
 */
public class ServiceNotFoundException extends NotFoundException {

    public ServiceNotFoundException(Object idOrName) {
        super("IT service " + idOrName + " was not found");
    }
}
