package com.iitsm.priority;

import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Alternative algorithm: the classic ITIL impact x urgency matrix. It ignores service criticality and
 * affected users, which makes it simpler but less precise than the weighted strategy.
 * <pre>
 *              Urgency HIGH   MEDIUM   LOW
 *   Impact HIGH        P1     P2       P3
 *          MEDIUM      P2     P3       P4
 *          LOW         P3     P4       P4
 * </pre>
 */
@Component
public class MatrixPriorityStrategy implements PriorityStrategy {

    public static final String NAME = "matrix";

    /** The matrix as a nested EnumMap: impact -> (urgency -> priority). */
    private static final Map<Impact, Map<Urgency, Priority>> MATRIX = new EnumMap<>(Impact.class);

    static {
        row(Impact.HIGH, Priority.P1, Priority.P2, Priority.P3);
        row(Impact.MEDIUM, Priority.P2, Priority.P3, Priority.P4);
        row(Impact.LOW, Priority.P3, Priority.P4, Priority.P4);
    }

    private static void row(Impact impact, Priority high, Priority medium, Priority low) {
        Map<Urgency, Priority> row = new EnumMap<>(Urgency.class);
        row.put(Urgency.HIGH, high);
        row.put(Urgency.MEDIUM, medium);
        row.put(Urgency.LOW, low);
        MATRIX.put(impact, row);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "ITIL impact x urgency matrix";
    }

    @Override
    public PriorityResult calculate(PriorityInput input) {
        Priority priority = MATRIX.get(input.impact()).get(input.urgency());
        return new PriorityResult(priority, "ITIL matrix: Impact " + input.impact().label()
                + " / Urgency " + input.urgency().label() + " -> " + priority.display());
    }
}
