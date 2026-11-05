package com.iitsm.domain.audit;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import com.iitsm.domain.incident.IncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One immutable line of the audit trail, e.g. "10:07 - INC1024 assigned to tech1 by system".
 * Stores the incident <i>number</i> as text instead of a foreign key, so the history stays intact
 * even if the incident is later changed or removed. Written by AuditListener (Milestone 6).
 */
@Entity
@Table(name = "audit_entry", indexes = @Index(columnList = "incidentNumber"))
public class AuditEntry extends BaseEntity {

    @Column(nullable = false)
    private Instant timestamp;

    /** Username of who did it, or "system" for automatic actions. */
    @Column(nullable = false, length = 50)
    private String actor;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 20)
    private String incidentNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentStatus newStatus;

    @Column(length = 1000)
    private String description;

    protected AuditEntry() {
    }

    public AuditEntry(Instant timestamp, String actor, String action, String incidentNumber,
                      IncidentStatus previousStatus, IncidentStatus newStatus, String description) {
        this.timestamp = Checks.requireNonNull(timestamp, "Audit timestamp");
        this.actor = Checks.requireText(actor, "Audit actor");
        this.action = Checks.requireText(action, "Audit action");
        this.incidentNumber = Checks.optionalText(incidentNumber);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.description = Checks.optionalText(description);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public IncidentStatus getPreviousStatus() {
        return previousStatus;
    }

    public IncidentStatus getNewStatus() {
        return newStatus;
    }

    public String getDescription() {
        return description;
    }
}
