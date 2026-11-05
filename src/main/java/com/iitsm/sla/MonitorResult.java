package com.iitsm.sla;

/**
 * What one SLA monitor run did (for the log and tests).
 */
public record MonitorResult(int breaches, int warnings, int escalations) {
}
