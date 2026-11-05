package com.iitsm.service;

import com.iitsm.domain.incident.Incident;

import java.util.List;
import java.util.Map;

/**
 * Figures for the administrator dashboard, computed by {@link ReportService}.
 *
 * @param slaCompliancePercent share of finished, SLA-tracked incidents that met their SLA (null if none)
 * @param averageResolutionMinutes mean time from report to resolution (null if none resolved)
 * @param byService   incident count per service, highest first
 * @param hotspots    services with several open incidents reported in the last hour (correlation)
 */
public record DashboardReport(long total,
                              long open,
                              long criticalOpen,
                              long resolved,
                              long slaViolations,
                              Integer slaCompliancePercent,
                              Long averageResolutionMinutes,
                              Map<String, Long> byService,
                              Map<String, Long> byCategory,
                              Map<String, Long> byPriority,
                              Map<String, Long> byStatus,
                              List<Incident> atRisk,
                              List<Incident> unassigned,
                              Map<String, Long> hotspots) {

    /** One titled breakdown for the dashboard, with its largest value for scaling bars. */
    public record Breakdown(String title, Map<String, Long> counts) {

        public long max() {
            return counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        }

        /** Bar width in percent for a value. */
        public long percent(long value) {
            long max = max();
            return max == 0 ? 0 : value * 100 / max;
        }
    }

    public List<Breakdown> breakdowns() {
        return List.of(new Breakdown("By service", byService), new Breakdown("By category", byCategory),
                new Breakdown("By priority", byPriority), new Breakdown("By status", byStatus));
    }
}
