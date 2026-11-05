package com.iitsm.sla;

import com.iitsm.domain.incident.Incident;

import java.time.Instant;

/**
 * Intelligent feature E - decides whether an incident is likely to miss its SLA, so people are warned
 * <i>before</i> the deadline. An interface so the rule can be replaced (e.g. by a model trained on
 * past resolution times).
 */
public interface SlaRiskEvaluator {

    /**
     * @param assigneeActiveIncidents current workload of the assigned technician (0 if unassigned)
     */
    boolean isAtRisk(Incident incident, Instant now, long assigneeActiveIncidents);
}
