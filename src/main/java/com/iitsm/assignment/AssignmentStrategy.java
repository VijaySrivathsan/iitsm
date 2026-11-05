package com.iitsm.assignment;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.user.Technician;

import java.util.List;
import java.util.Optional;

/**
 * Strategy pattern: an interchangeable rule for choosing a technician. An administrator can switch
 * the active strategy at runtime without any code change (see {@link AssignmentStrategyRegistry}).
 *
 * <p>Candidates passed in are already eligible (active, available, below their maximum workload).
 */
public interface AssignmentStrategy {

    /** Short name used in settings and the admin screen, e.g. "skill". */
    String name();

    /** One-line description shown to administrators. */
    String description();

    /** Candidates this strategy would accept, best first. Must not change any state. */
    List<TechnicianScore> rank(Incident incident, List<Candidate> candidates);

    /** The best candidate, if any. */
    default Optional<Technician> select(Incident incident, List<Candidate> candidates) {
        return rank(incident, candidates).stream().findFirst().map(TechnicianScore::technician);
    }

    /** Called after an incident was actually assigned using this strategy (for stateful strategies). */
    default void recordAssignment(Incident incident, Technician technician) {
        // most strategies keep no state
    }
}
