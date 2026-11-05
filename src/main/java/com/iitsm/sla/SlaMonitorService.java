package com.iitsm.sla;

import com.iitsm.assignment.AssignmentService;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.event.IncidentEventPublisher;
import com.iitsm.event.IncidentEventType;
import com.iitsm.intelligence.EscalationService;
import com.iitsm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;

/**
 * One pass of continuous SLA monitoring (run every minute by {@link SlaMonitor}):
 * for every open incident, detect breaches (SLA_VIOLATED) and SLA risk (SLA_APPROACHING, feature E),
 * then escalate overdue unacknowledged major incidents (feature C). Each warning/breach is reported
 * only once.
 */
@Service
@Transactional
public class SlaMonitorService {

    private static final EnumSet<IncidentStatus> MONITORED = EnumSet.of(IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS,
            IncidentStatus.PENDING_INFO);

    private final IncidentRepository incidents;
    private final SlaRiskEvaluator riskEvaluator;
    private final AssignmentService assignment;
    private final EscalationService escalation;
    private final IncidentEventPublisher events;
    private final Clock clock;

    public SlaMonitorService(IncidentRepository incidents, SlaRiskEvaluator riskEvaluator,
                             AssignmentService assignment, EscalationService escalation,
                             IncidentEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.riskEvaluator = riskEvaluator;
        this.assignment = assignment;
        this.escalation = escalation;
        this.events = events;
        this.clock = clock;
    }

    public MonitorResult runChecks() {
        Instant now = Instant.now(clock);
        int breaches = 0;
        int warnings = 0;
        for (Incident incident : incidents.findByStatusInOrderByCreatedAtAsc(MONITORED)) {
            if (incident.checkSlaBreach(now)) {
                breaches++;
                events.publish(IncidentEventType.SLA_VIOLATED, incident, incident.getStatus(), null,
                        "Deadline passed at " + incident.getSlaRecord().getBreachedAt());
                continue;
            }
            long load = incident.getAssignee() == null ? 0 : assignment.activeWorkload(incident.getAssignee());
            if (riskEvaluator.isAtRisk(incident, now, load) && incident.markSlaAtRisk()) {
                warnings++;
                events.publish(IncidentEventType.SLA_APPROACHING, incident, incident.getStatus(), null,
                        "Resolution due " + incident.getSlaRecord().getResolutionDue());
            }
        }
        int escalated = escalation.escalateOverdue(now).size();
        return new MonitorResult(breaches, warnings, escalated);
    }
}
