package com.iitsm.assignment;

import com.iitsm.domain.user.Technician;

/**
 * One entry in a strategy's ranking: who, how good a match (higher is better) and why.
 */
public record TechnicianScore(Technician technician, double score, int activeIncidents, String reason) {
}
