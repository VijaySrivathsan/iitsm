package com.iitsm.domain.incident;

/**
 * The lifecycle operations that can be requested on an incident. Which ones are allowed depends on
 * the incident's current state (State pattern).
 */
public enum IncidentOperation {
    CLASSIFY("Classify"),
    ASSIGN("Assign"),
    ACKNOWLEDGE("Acknowledge"),
    START_WORK("Start work on"),
    REQUEST_INFO("Request information on"),
    PROVIDE_INFO("Provide information on"),
    RESOLVE("Resolve"),
    VERIFY("Verify"),
    REOPEN("Reopen"),
    CLOSE("Close");

    private final String label;

    IncidentOperation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
