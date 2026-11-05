package com.iitsm.domain.sla;

/**
 * Where an incident stands against its SLA.
 */
public enum SlaStatus {
    /** No SLA applied yet (incident not classified). */
    NOT_STARTED,
    ON_TRACK,
    /** Close to a deadline - a warning has been or should be raised. */
    AT_RISK,
    /** A deadline was missed. */
    BREACHED,
    /** Resolved within the resolution deadline. */
    MET
}
