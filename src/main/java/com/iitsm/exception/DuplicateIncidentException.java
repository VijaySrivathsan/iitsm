package com.iitsm.exception;

/**
 * Thrown when an employee reports the same problem again shortly after their own identical report.
 */
public class DuplicateIncidentException extends ItsmException {

    public DuplicateIncidentException(String existingNumber) {
        super("You already reported this problem as " + existingNumber
                + " within the last hour. Please add details to that incident instead.");
    }
}
