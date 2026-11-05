package com.iitsm.event;

/**
 * Everything that can happen to an incident and that other parts of the system may react to
 * (audit, notifications, escalation).
 */
public enum IncidentEventType {
    CREATED("Incident created"),
    CLASSIFIED("Incident classified"),
    PRIORITY_CHANGED("Priority changed"),
    ASSIGNED("Technician assigned"),
    ACKNOWLEDGED("Technician acknowledged incident"),
    WORK_STARTED("Investigation started"),
    INFO_REQUESTED("Information requested from reporter"),
    INFO_PROVIDED("Reporter provided information"),
    NOTE_ADDED("Note added"),
    RESOLVED("Incident resolved"),
    VERIFIED("Resolution verified by reporter"),
    REOPENED("Incident reopened"),
    CLOSED("Incident closed"),
    SLA_APPROACHING("SLA deadline approaching"),
    SLA_VIOLATED("SLA violated"),
    ESCALATED("Incident escalated"),
    DUPLICATE_DETECTED("Possible duplicate detected");

    private final String label;

    IncidentEventType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
