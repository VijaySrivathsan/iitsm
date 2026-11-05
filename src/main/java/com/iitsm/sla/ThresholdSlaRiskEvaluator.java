package com.iitsm.sla;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.sla.SlaRecord;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.domain.user.Technician;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * An incident is AT RISK when
 * <ul>
 *   <li>75% (configurable) of the response window has passed and nobody has acknowledged it, or</li>
 *   <li>75% of the resolution window has passed, or</li>
 *   <li>50% of the resolution window has passed and the assigned technician is at least 80% busy.</li>
 * </ul>
 */
@Component
public class ThresholdSlaRiskEvaluator implements SlaRiskEvaluator {

    static final double BUSY_ELAPSED = 0.5;
    static final double BUSY_LOAD = 0.8;

    private final double threshold;

    public ThresholdSlaRiskEvaluator(@Value("${itsm.sla.risk-threshold:0.75}") double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean isAtRisk(Incident incident, Instant now, long assigneeActiveIncidents) {
        SlaRecord sla = incident.getSlaRecord();
        if (!sla.isStarted() || sla.getStatus() != SlaStatus.ON_TRACK || !incident.getStatus().isOpen()) {
            return false;
        }
        if (sla.getRespondedAt() == null && fraction(incident.getCreatedAt(), sla.getResponseDue(), now) >= threshold) {
            return true;
        }
        double resolution = sla.elapsedFraction(incident.getCreatedAt(), now);
        if (resolution >= threshold) {
            return true;
        }
        Technician tech = incident.getAssignee();
        return tech != null && resolution >= BUSY_ELAPSED
                && assigneeActiveIncidents >= BUSY_LOAD * tech.getMaxWorkload();
    }

    static double fraction(Instant start, Instant due, Instant now) {
        long total = Duration.between(start, due).toMillis();
        return total <= 0 ? 1 : (double) Duration.between(start, now).toMillis() / total;
    }
}
