package com.iitsm.domain.sla;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;

/**
 * The SLA timings of one incident: deadlines and when they were actually met.
 * An {@code @Embeddable} value that lives inside the Incident row - <b>composition</b>: it has no
 * identity or life of its own. SLA rules that fill it in are added in Milestone 4.
 */
@Embeddable
public class SlaRecord {

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status", length = 20)
    private SlaStatus status;

    @Column(name = "sla_response_due")
    private Instant responseDue;

    @Column(name = "sla_resolution_due")
    private Instant resolutionDue;

    @Column(name = "sla_responded_at")
    private Instant respondedAt;

    @Column(name = "sla_resolved_at")
    private Instant resolvedAt;

    @Column(name = "sla_breached_at")
    private Instant breachedAt;

    @Column(name = "sla_warning_sent")
    private Boolean warningSent;

    protected SlaRecord() {
    }

    /** A record for an incident whose SLA has not been applied yet. */
    public static SlaRecord notStarted() {
        SlaRecord record = new SlaRecord();
        record.status = SlaStatus.NOT_STARTED;
        record.warningSent = false;
        return record;
    }

    public SlaStatus getStatus() {
        return status == null ? SlaStatus.NOT_STARTED : status;
    }

    public Instant getResponseDue() {
        return responseDue;
    }

    public Instant getResolutionDue() {
        return resolutionDue;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getBreachedAt() {
        return breachedAt;
    }

    public boolean isWarningSent() {
        return Boolean.TRUE.equals(warningSent);
    }
}
