package com.iitsm.priority;

import com.iitsm.domain.Checks;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.Urgency;

/**
 * Everything a {@link PriorityStrategy} may use to decide priority. A small value object, so strategies
 * can be tested without building a whole incident.
 */
public record PriorityInput(Impact impact, Urgency urgency, int businessCriticality, int affectedUsers) {

    public PriorityInput {
        Checks.requireNonNull(impact, "Impact");
        Checks.requireNonNull(urgency, "Urgency");
    }

    public static PriorityInput from(Incident incident) {
        return new PriorityInput(incident.getImpact(), incident.getUrgency(),
                incident.getService().getBusinessCriticality(), incident.getAffectedUsers());
    }

    /** Same input with a different impact and urgency (used when a reporter updates them). */
    public PriorityInput with(Impact newImpact, Urgency newUrgency) {
        return new PriorityInput(newImpact, newUrgency, businessCriticality, affectedUsers);
    }
}
