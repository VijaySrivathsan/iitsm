package com.iitsm.domain.escalation;

/**
 * The escalation chain from the brief: Technician -> Team Lead -> System Administrator -> IT Manager.
 * (The technician is level 0, the person the incident is already assigned to.)
 */
public enum EscalationLevel {
    TEAM_LEAD(1, "Team lead"),
    ADMINISTRATOR(2, "System administrator"),
    IT_MANAGER(3, "IT manager");

    private final int step;
    private final String label;

    EscalationLevel(int step, String label) {
        this.step = step;
        this.label = label;
    }

    public int step() {
        return step;
    }

    public String label() {
        return label;
    }
}
