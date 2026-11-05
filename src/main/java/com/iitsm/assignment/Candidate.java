package com.iitsm.assignment;

import com.iitsm.domain.Checks;
import com.iitsm.domain.user.Technician;

/**
 * A technician who could take an incident, together with the numbers a strategy needs. Prepared by
 * {@link AssignmentService} so strategies stay pure, database-free and easy to test.
 *
 * @param activeIncidents   incidents currently occupying the technician (their workload)
 * @param handledOnService  incidents on the same IT service they have resolved before
 */
public record Candidate(Technician technician, int activeIncidents, int handledOnService) {

    public Candidate {
        Checks.requireNonNull(technician, "Technician");
    }

    /** Workload as a fraction of capacity: 0.0 = idle, 1.0 = full. */
    public double loadRatio() {
        return (double) activeIncidents / technician.getMaxWorkload();
    }

    public String username() {
        return technician.getUsername();
    }
}
