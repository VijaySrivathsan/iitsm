package com.iitsm.domain.incident;

import com.iitsm.domain.Checks;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.user.Employee;
import com.iitsm.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Factory pattern: turns an employee's {@link IncidentReport} into a valid NEW {@link Incident}.
 * Centralises the creation rules - defaults (impact/urgency MEDIUM, 1 affected user), length limits,
 * a title derived from the description if none was given, and the creation time from the shared
 * {@link Clock} - so every incident is created the same way.
 */
@Component
public class IncidentFactory {

    public static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final int DERIVED_TITLE_LENGTH = 80;

    private final Clock clock;

    public IncidentFactory(Clock clock) {
        this.clock = clock;
    }

    public Incident create(IncidentNumber number, IncidentReport report, Employee reporter, ITService service) {
        Checks.requireNonNull(report, "Incident report");
        String description = Checks.requireText(report.description(), "Description");
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidInputException("Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (!service.isActive()) {
            throw new InvalidInputException("Service " + service.getName() + " is no longer supported");
        }
        String title = report.title() == null || report.title().isBlank()
                ? deriveTitle(description)
                : report.title();
        Impact impact = report.impact() == null ? Impact.MEDIUM : report.impact();
        Urgency urgency = report.urgency() == null ? Urgency.MEDIUM : report.urgency();
        int affectedUsers = report.affectedUsers() == null ? 1 : report.affectedUsers();

        return new Incident(number, title, description, reporter, service, impact, urgency, affectedUsers,
                Instant.now(clock));
    }

    /** First sentence (or first 80 characters) of the description. */
    private static String deriveTitle(String description) {
        String firstLine = description.split("[.\\n]", 2)[0].trim();
        if (firstLine.isEmpty()) {
            firstLine = description;
        }
        return firstLine.length() <= DERIVED_TITLE_LENGTH
                ? firstLine
                : firstLine.substring(0, DERIVED_TITLE_LENGTH - 3) + "...";
    }
}
