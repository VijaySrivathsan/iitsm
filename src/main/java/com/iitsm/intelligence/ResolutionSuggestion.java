package com.iitsm.intelligence;

import com.iitsm.domain.incident.Incident;

/**
 * A past incident whose resolution might help with the current one.
 */
public record ResolutionSuggestion(Incident pastIncident, double similarity) {
}
