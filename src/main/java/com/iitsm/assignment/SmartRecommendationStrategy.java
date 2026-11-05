package com.iitsm.assignment;

import com.iitsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Intelligent feature D - smart technician recommendation. Scores every candidate out of 100:
 * <pre>
 *   40 x skill match (1 if skilled in the category, else 0)
 * + 25 x spare capacity (1 - active / max)
 * + 15 x experience / 5
 * + 10 x min(incidents handled on this service, 5) / 5
 * + 10    if the incident is P1/P2 and the technician is senior (experience >= 4)
 * </pre>
 * and explains the score, so an administrator can see why someone was recommended.
 */
@Component
public class SmartRecommendationStrategy implements AssignmentStrategy {

    public static final String NAME = "smart";

    static final double SKILL_WEIGHT = 40;
    static final double CAPACITY_WEIGHT = 25;
    static final double EXPERIENCE_WEIGHT = 15;
    static final double SERVICE_HISTORY_WEIGHT = 10;
    static final double SENIOR_FOR_MAJOR_BONUS = 10;
    static final int SERVICE_HISTORY_CAP = 5;
    static final int SENIOR_LEVEL = 4;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Weighted score of skill, spare capacity, experience and history with the service";
    }

    @Override
    public List<TechnicianScore> rank(Incident incident, List<Candidate> candidates) {
        List<TechnicianScore> scores = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) {
            scores.add(score(incident, c));
        }
        scores.sort(Comparator.comparingDouble(TechnicianScore::score).reversed()
                .thenComparing(s -> s.technician().getUsername()));
        return scores;
    }

    TechnicianScore score(Incident incident, Candidate c) {
        boolean skilled = c.technician().hasSkill(incident.getCategory());
        double skill = skilled ? SKILL_WEIGHT : 0;
        double capacity = CAPACITY_WEIGHT * Math.max(0, 1 - c.loadRatio());
        double experience = EXPERIENCE_WEIGHT * c.technician().getExperienceLevel() / 5.0;
        double history = SERVICE_HISTORY_WEIGHT * Math.min(c.handledOnService(), SERVICE_HISTORY_CAP)
                / SERVICE_HISTORY_CAP;
        boolean seniorForMajor = incident.getPriority() != null && incident.getPriority().isMajor()
                && c.technician().getExperienceLevel() >= SENIOR_LEVEL;
        double bonus = seniorForMajor ? SENIOR_FOR_MAJOR_BONUS : 0;
        double total = skill + capacity + experience + history + bonus;

        String reason = String.format(Locale.ROOT,
                "skill %.0f%s + capacity %.1f (%d/%d busy) + experience %.0f (level %d) + service history %.0f (%d)%s = %.1f",
                skill, skilled ? "" : " (no " + incident.getCategory() + " skill)",
                capacity, c.activeIncidents(), c.technician().getMaxWorkload(),
                experience, c.technician().getExperienceLevel(), history, c.handledOnService(),
                seniorForMajor ? " + senior bonus 10" : "", total);
        return new TechnicianScore(c.technician(), total, c.activeIncidents(), reason);
    }
}
