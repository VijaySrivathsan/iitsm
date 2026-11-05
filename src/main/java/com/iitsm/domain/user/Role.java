package com.iitsm.domain.user;

/**
 * Security role of a user. Each {@link User} subtype maps to exactly one role.
 */
public enum Role {
    EMPLOYEE("Employee"),
    TECHNICIAN("Technician"),
    ADMIN("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Spring Security authority name, e.g. {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
