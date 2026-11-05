package com.iitsm.domain.incident;

/**
 * Persisted lifecycle status of an incident. Which transitions are legal is decided by the
 * State pattern classes in {@code domain.incident.state} (Milestone 3), not by this enum.
 */
public enum IncidentStatus {
    NEW,
    CLASSIFIED,
    ASSIGNED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    PENDING_INFO,
    RESOLVED,
    VERIFIED,
    CLOSED;

    /** True while the incident still needs work from IT (before it is resolved). */
    public boolean isOpen() {
        return this != RESOLVED && this != VERIFIED && this != CLOSED;
    }

    /** True when the incident occupies a technician and counts towards their workload. */
    public boolean countsTowardsWorkload() {
        return this == ASSIGNED || this == ACKNOWLEDGED || this == IN_PROGRESS || this == PENDING_INFO;
    }
}
