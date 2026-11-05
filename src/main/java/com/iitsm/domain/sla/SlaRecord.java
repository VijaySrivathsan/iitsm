package com.iitsm.domain.sla;

import com.iitsm.domain.Checks;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Duration;
import java.time.Instant;

/**
 * The SLA timings of one incident: deadlines, when they were actually met, and the resulting status.
 * An {@code @Embeddable} value that lives inside the Incident row - <b>composition</b>: it has no
 * identity or life of its own. The owning Incident calls these methods as it moves through its
 * lifecycle; nothing else should.
 *
 * <p>Rules: a deadline met exactly on time is <i>not</i> a breach; a breach is permanent (reopening
 * or resolving later never turns BREACHED back into ON_TRACK or MET).
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

    // ----- changes (called by Incident) -----

    /**
     * Sets the deadlines, measured from when the incident was reported. Called on classification and
     * again if the priority changes; already-recorded response/resolution times are kept and re-checked.
     */
    public void start(Instant reportedAt, Duration responseTarget, Duration resolutionTarget) {
        Checks.requireNonNull(reportedAt, "Reported time");
        this.responseDue = reportedAt.plus(Checks.requireNonNull(responseTarget, "Response target"));
        this.resolutionDue = reportedAt.plus(Checks.requireNonNull(resolutionTarget, "Resolution target"));
        if (status == null || status == SlaStatus.NOT_STARTED) {
            this.status = SlaStatus.ON_TRACK;
        }
        if (respondedAt != null && respondedAt.isAfter(responseDue)) {
            breach(respondedAt);
        }
        if (resolvedAt != null) {
            settleResolution();
        }
    }

    /** The technician acknowledged the incident. Returns true if this act breached the SLA. */
    public boolean recordResponse(Instant at) {
        if (!isStarted() || respondedAt != null) {
            return false; // response time is only measured once
        }
        this.respondedAt = Checks.requireNonNull(at, "Response time");
        if (at.isAfter(responseDue)) {
            return breach(at);
        }
        return false;
    }

    /** The incident was resolved. Returns true if this act breached the SLA. */
    public boolean recordResolution(Instant at) {
        if (!isStarted()) {
            return false;
        }
        this.resolvedAt = Checks.requireNonNull(at, "Resolution time");
        return settleResolution();
    }

    /** The incident was reopened: the resolution clock runs again. Returns true if now breached. */
    public boolean reopen(Instant now) {
        if (!isStarted()) {
            return false;
        }
        this.resolvedAt = null;
        if (status == SlaStatus.MET) {
            status = SlaStatus.ON_TRACK;
        }
        return checkBreach(now);
    }

    /**
     * Checks the deadlines against the current time (used by the SLA monitor). Returns true only the
     * first time a breach is detected, so the violation is reported once.
     */
    public boolean checkBreach(Instant now) {
        if (!isStarted() || status == SlaStatus.BREACHED || status == SlaStatus.MET) {
            return false;
        }
        if (respondedAt == null && now.isAfter(responseDue)) {
            return breach(responseDue);
        }
        if (resolvedAt == null && now.isAfter(resolutionDue)) {
            return breach(resolutionDue);
        }
        return false;
    }

    /** Marks the incident as close to a deadline. Returns true the first time (so one warning is sent). */
    public boolean markAtRisk() {
        if (status != SlaStatus.ON_TRACK) {
            return false;
        }
        status = SlaStatus.AT_RISK;
        boolean firstWarning = !isWarningSent();
        warningSent = true;
        return firstWarning;
    }

    private boolean settleResolution() {
        if (resolvedAt.isAfter(resolutionDue)) {
            return breach(resolvedAt);
        }
        if (status != SlaStatus.BREACHED) {
            status = SlaStatus.MET;
        }
        return false;
    }

    private boolean breach(Instant at) {
        if (status == SlaStatus.BREACHED) {
            return false;
        }
        status = SlaStatus.BREACHED;
        breachedAt = at;
        return true;
    }

    // ----- queries -----

    public boolean isStarted() {
        return responseDue != null && resolutionDue != null;
    }

    /** Fraction (0..1+) of the resolution window used so far; 0 if not started. */
    public double elapsedFraction(Instant reportedAt, Instant now) {
        if (!isStarted()) {
            return 0;
        }
        long total = Duration.between(reportedAt, resolutionDue).toMillis();
        long used = Duration.between(reportedAt, now).toMillis();
        return total <= 0 ? 1 : Math.max(0, (double) used / total);
    }

    /** Time left until the resolution deadline (negative if overdue). Null if not started. */
    public Duration timeToResolutionDeadline(Instant now) {
        return isStarted() ? Duration.between(now, resolutionDue) : null;
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
