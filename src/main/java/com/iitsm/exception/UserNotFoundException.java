package com.iitsm.exception;

/**
 * Thrown when a user id or username does not exist.
 */
public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Object idOrUsername) {
        super("User " + idOrUsername + " was not found");
    }
}
