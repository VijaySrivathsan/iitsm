package com.iitsm.domain.incident;

/**
 * Incident priority, P1 being the most urgent. Calculated by a PriorityStrategy (Milestone 4).
 */
public enum Priority {
    P1("Critical"),
    P2("High"),
    P3("Medium"),
    P4("Low");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** P1 and P2 are "major" incidents: they are eligible for automatic escalation. */
    public boolean isMajor() {
        return this == P1 || this == P2;
    }

    public String display() {
        return name() + " - " + label;
    }
}
