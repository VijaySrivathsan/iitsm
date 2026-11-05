package com.iitsm.priority;

import com.iitsm.domain.Checks;
import com.iitsm.domain.incident.Priority;

/**
 * The outcome of a priority calculation: the priority plus a human-readable explanation of how it
 * was reached (shown on the incident page so users can see why it is P1).
 */
public record PriorityResult(Priority priority, String explanation) {

    public PriorityResult {
        Checks.requireNonNull(priority, "Priority");
        Checks.requireText(explanation, "Explanation");
    }
}
