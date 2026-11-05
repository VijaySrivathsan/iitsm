package com.iitsm.domain.sla;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Priority;
import com.iitsm.exception.InvalidInputException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Duration;

/**
 * Response and resolution targets for one priority, either for a specific service or - when
 * {@code service} is null - the default for all services.
 */
@Entity
@Table(name = "sla_policy", uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "priority"}))
public class SlaPolicy extends BaseEntity {

    /** Association: a policy refers to a service but does not own it. Null = default policy. */
    @ManyToOne
    @JoinColumn(name = "service_id")
    private ITService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Priority priority;

    private int responseMinutes;

    private int resolutionMinutes;

    protected SlaPolicy() {
    }

    public SlaPolicy(ITService service, Priority priority, int responseMinutes, int resolutionMinutes) {
        this.service = service;
        this.priority = Checks.requireNonNull(priority, "Priority");
        changeTargets(responseMinutes, resolutionMinutes);
    }

    /** Creates the default policy for a priority (applies to services without their own policy). */
    public static SlaPolicy defaultFor(Priority priority, int responseMinutes, int resolutionMinutes) {
        return new SlaPolicy(null, priority, responseMinutes, resolutionMinutes);
    }

    public void changeTargets(int responseMinutes, int resolutionMinutes) {
        if (responseMinutes <= 0 || resolutionMinutes <= 0) {
            throw new InvalidInputException("SLA targets must be positive");
        }
        if (resolutionMinutes < responseMinutes) {
            throw new InvalidInputException("Resolution target cannot be shorter than response target");
        }
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public boolean isDefault() {
        return service == null;
    }

    public Duration responseTarget() {
        return Duration.ofMinutes(responseMinutes);
    }

    public Duration resolutionTarget() {
        return Duration.ofMinutes(resolutionMinutes);
    }

    public ITService getService() {
        return service;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }
}
