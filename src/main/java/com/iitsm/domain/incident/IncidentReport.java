package com.iitsm.domain.incident;

/**
 * What an employee fills in when reporting a problem. Passed to {@link IncidentFactory}.
 *
 * @param affectedUsers optional; treated as 1 when null
 */
public record IncidentReport(String title,
                             String description,
                             Impact impact,
                             Urgency urgency,
                             Integer affectedUsers) {
}
