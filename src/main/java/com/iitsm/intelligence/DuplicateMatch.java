package com.iitsm.intelligence;

import com.iitsm.domain.incident.Incident;

/**
 * An existing incident that looks like the same problem, with its similarity score.
 */
public record DuplicateMatch(Incident incident, double score) {
}
