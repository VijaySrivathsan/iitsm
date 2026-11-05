package com.iitsm.assignment;

import com.iitsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Spreads work evenly: the technician with the fewest active incidents wins, whatever their skills.
 * Uses a {@link PriorityQueue} (a min-heap ordered by workload), the natural data structure for
 * "always take the smallest".
 */
@Component
public class LoadBasedAssignmentStrategy implements AssignmentStrategy {

    public static final String NAME = "load";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Least busy technician first, regardless of skills";
    }

    @Override
    public List<TechnicianScore> rank(Incident incident, List<Candidate> candidates) {
        PriorityQueue<Candidate> byLoad = new PriorityQueue<>(
                Comparator.comparingInt(Candidate::activeIncidents).thenComparing(Candidate::username));
        byLoad.addAll(candidates);

        List<TechnicianScore> ranking = new ArrayList<>(candidates.size());
        while (!byLoad.isEmpty()) {
            Candidate c = byLoad.poll();
            ranking.add(new TechnicianScore(c.technician(), 100 - c.activeIncidents(), c.activeIncidents(),
                    c.activeIncidents() + " active incident(s) of max " + c.technician().getMaxWorkload()));
        }
        return ranking;
    }
}
