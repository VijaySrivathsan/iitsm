package com.iitsm.domain.incident;

import com.iitsm.domain.Checks;
import com.iitsm.domain.user.Technician;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * How an incident was fixed. An immutable value embedded in the incident row (composition).
 */
@Embeddable
public class Resolution {

    @Column(name = "resolution_summary", length = 2000)
    private String summary;

    @Column(name = "resolution_root_cause", length = 1000)
    private String rootCause;

    @ManyToOne
    @JoinColumn(name = "resolved_by_id")
    private Technician resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Resolution() {
    }

    public Resolution(String summary, String rootCause, Technician resolvedBy, Instant resolvedAt) {
        this.summary = Checks.requireText(summary, "Resolution summary");
        this.rootCause = Checks.optionalText(rootCause);
        this.resolvedBy = Checks.requireNonNull(resolvedBy, "Resolving technician");
        this.resolvedAt = Checks.requireNonNull(resolvedAt, "Resolution time");
    }

    public String getSummary() {
        return summary;
    }

    public String getRootCause() {
        return rootCause;
    }

    public Technician getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
