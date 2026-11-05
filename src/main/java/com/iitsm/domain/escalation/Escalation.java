package com.iitsm.domain.escalation;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Record that an unacknowledged major incident was escalated to someone more senior. Immutable.
 */
@Entity
@Table(name = "escalation")
public class Escalation extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscalationLevel level;

    @ManyToOne(optional = false)
    @JoinColumn(name = "escalated_to_id")
    private User escalatedTo;

    @Column(nullable = false)
    private Instant escalatedAt;

    @Column(length = 500)
    private String reason;

    protected Escalation() {
    }

    public Escalation(Incident incident, EscalationLevel level, User escalatedTo, Instant escalatedAt, String reason) {
        this.incident = Checks.requireNonNull(incident, "Incident");
        this.level = Checks.requireNonNull(level, "Level");
        this.escalatedTo = Checks.requireNonNull(escalatedTo, "Escalation target");
        this.escalatedAt = Checks.requireNonNull(escalatedAt, "Escalation time");
        this.reason = Checks.optionalText(reason);
    }

    public Incident getIncident() {
        return incident;
    }

    public EscalationLevel getLevel() {
        return level;
    }

    public User getEscalatedTo() {
        return escalatedTo;
    }

    public Instant getEscalatedAt() {
        return escalatedAt;
    }

    public String getReason() {
        return reason;
    }
}
