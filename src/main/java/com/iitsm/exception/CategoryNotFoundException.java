package com.iitsm.exception;

/**
 * Thrown when an incident category id or name does not exist.
 */
public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException(Object idOrName) {
        super("Category " + idOrName + " was not found");
    }
}
