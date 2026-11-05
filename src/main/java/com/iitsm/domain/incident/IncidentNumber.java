package com.iitsm.domain.incident;

import com.iitsm.exception.InvalidInputException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object for the human-readable incident number shown to users, e.g. {@code INC1001}.
 * Separate from the database id so numbers stay meaningful and never expose internal keys.
 */
public record IncidentNumber(long sequence) {

    public static final String PREFIX = "INC";
    public static final long FIRST_SEQUENCE = 1001;
    private static final Pattern FORMAT = Pattern.compile("(?i)^\\s*" + PREFIX + "(\\d+)\\s*$");

    public IncidentNumber {
        if (sequence < 1) {
            throw new InvalidInputException("Incident sequence must be positive");
        }
    }

    /** Parses text such as "INC1024" (case-insensitive). */
    public static IncidentNumber parse(String text) {
        if (text == null) {
            throw new InvalidInputException("Incident number is required");
        }
        Matcher m = FORMAT.matcher(text);
        if (!m.matches()) {
            throw new InvalidInputException("Not a valid incident number: " + text);
        }
        return new IncidentNumber(Long.parseLong(m.group(1)));
    }

    /** Next number after the highest one already used (or the first number if none exist). */
    public static IncidentNumber next(Long currentMaxSequence) {
        return new IncidentNumber(currentMaxSequence == null ? FIRST_SEQUENCE : currentMaxSequence + 1);
    }

    @Override
    public String toString() {
        return PREFIX + sequence;
    }
}
