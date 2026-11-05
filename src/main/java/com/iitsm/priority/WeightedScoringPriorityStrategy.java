package com.iitsm.priority;

import com.iitsm.domain.incident.Priority;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Default priority algorithm (docs/plan.md section 5):
 * <pre>
 *   base  = impact (1-3) x urgency (1-3) x business criticality (1-5)      -> 1..45
 *   score = base x affected-users factor (1.0 / 1.2 / 1.5 / 2.0)
 *   P1 if score >= 30, P2 if >= 15, P3 if >= 6, otherwise P4
 * </pre>
 * Example: payment outage, HIGH x HIGH x 5 = 45, x 1.5 for 500 users = 67.5 -> P1.
 */
@Component
public class WeightedScoringPriorityStrategy implements PriorityStrategy {

    public static final String NAME = "weighted";

    static final double P1_THRESHOLD = 30;
    static final double P2_THRESHOLD = 15;
    static final double P3_THRESHOLD = 6;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Impact x urgency x service criticality, scaled by number of affected users";
    }

    @Override
    public PriorityResult calculate(PriorityInput input) {
        int base = input.impact().weight() * input.urgency().weight() * input.businessCriticality();
        double factor = affectedUsersFactor(input.affectedUsers());
        double score = base * factor;
        Priority priority = toPriority(score);
        String explanation = String.format(Locale.ROOT,
                "Impact %s(%d) x Urgency %s(%d) x Criticality %d = %d; x %.1f for %d affected user(s) = %.1f -> %s",
                input.impact().label(), input.impact().weight(),
                input.urgency().label(), input.urgency().weight(),
                input.businessCriticality(), base, factor, input.affectedUsers(), score, priority.display());
        return new PriorityResult(priority, explanation);
    }

    static double affectedUsersFactor(int affectedUsers) {
        if (affectedUsers >= 1000) {
            return 2.0;
        }
        if (affectedUsers >= 100) {
            return 1.5;
        }
        if (affectedUsers >= 10) {
            return 1.2;
        }
        return 1.0;
    }

    static Priority toPriority(double score) {
        if (score >= P1_THRESHOLD) {
            return Priority.P1;
        }
        if (score >= P2_THRESHOLD) {
            return Priority.P2;
        }
        if (score >= P3_THRESHOLD) {
            return Priority.P3;
        }
        return Priority.P4;
    }
}
