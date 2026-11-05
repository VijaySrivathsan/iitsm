package com.iitsm.assignment;

import com.iitsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Only technicians who have the incident's category as a skill. Among them, the least busy wins;
 * ties go to the more experienced technician.
 */
@Component
public class SkillBasedAssignmentStrategy implements AssignmentStrategy {

    public static final String NAME = "skill";

    private static final Comparator<Candidate> LEAST_LOADED_THEN_MOST_EXPERIENCED =
            Comparator.comparingInt(Candidate::activeIncidents)
                    .thenComparing(c -> c.technician().getExperienceLevel(), Comparator.reverseOrder())
                    .thenComparing(Candidate::username);

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Technicians skilled in the incident's category; least busy first, then most experienced";
    }

    @Override
    public List<TechnicianScore> rank(Incident incident, List<Candidate> candidates) {
        return candidates.stream()
                .filter(c -> c.technician().hasSkill(incident.getCategory()))
                .sorted(LEAST_LOADED_THEN_MOST_EXPERIENCED)
                .map(c -> new TechnicianScore(c.technician(), 100 - c.activeIncidents(), c.activeIncidents(),
                        "Skilled in " + incident.getCategory() + ", " + c.activeIncidents() + " active incident(s)"))
                .toList();
    }
}
