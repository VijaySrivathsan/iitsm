package com.iitsm.event;

import com.iitsm.domain.incident.IncidentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Observer pattern: the "subject" (IncidentService, later also the SLA monitor) publishes one of
 * these after every change, and any number of "observers" (audit, notifications, escalation)
 * listen for it. The publisher never needs to know who is listening, so a new reaction can be added
 * without touching the code that changes incidents.
 *
 * <p>An immutable record holding only plain values (ids, numbers, usernames), so listeners cannot
 * accidentally modify the incident through the event.
 *
 * @param actor username of who caused it, or {@link #SYSTEM} for automatic actions
 */
public record IncidentEvent(IncidentEventType type,
                            Long incidentId,
                            String incidentNumber,
                            IncidentStatus previousStatus,
                            IncidentStatus newStatus,
                            String actor,
                            String description,
                            Instant occurredAt) {

    public static final String SYSTEM = "system";

    public IncidentEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(incidentNumber, "incidentNumber");
        Objects.requireNonNull(occurredAt, "occurredAt");
        actor = actor == null || actor.isBlank() ? SYSTEM : actor;
    }

    /** True if the event moved the incident to a different status. */
    public boolean isStatusChange() {
        return previousStatus != newStatus;
    }
}
