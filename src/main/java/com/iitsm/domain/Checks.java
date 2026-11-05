package com.iitsm.domain;

import com.iitsm.exception.InvalidInputException;

/**
 * Small guard helpers used by domain constructors and methods to protect their invariants.
 * Every failure becomes an {@link InvalidInputException} with a readable message.
 */
public final class Checks {

    private Checks() {
    }

    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(field + " must not be blank");
        }
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new InvalidInputException(field + " is required");
        }
        return value;
    }

    public static int requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new InvalidInputException(field + " must be between " + min + " and " + max + " but was " + value);
        }
        return value;
    }

    public static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
