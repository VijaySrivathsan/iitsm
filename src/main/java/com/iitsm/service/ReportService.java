package com.iitsm.service;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.User;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the dashboard figures (incident overview, breakdowns per service/category/priority/status,
 * SLA compliance, average resolution time, at-risk incidents and service hotspots).
 *
 * <p>Uses Java streams and {@code Collectors.groupingBy} over the incident list - simple and fine for
 * thousands of incidents. At larger scale these would become GROUP BY queries in the repository.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    /** Number of open incidents on one service within the last hour that marks a hotspot. */
    static final int HOTSPOT_THRESHOLD = 2;

    private final IncidentRepository incidents;
    private final Clock clock;

    public ReportService(IncidentRepository incidents, Clock clock) {
        this.incidents = incidents;
        this.clock = clock;
    }

    public DashboardReport overview(User viewer) {
        if (!(viewer instanceof Administrator)) {
            throw new UnauthorizedOperationException("Only administrators can view reports");
        }
        List<Incident> all = incidents.findAll();
        Instant now = Instant.now(clock);

        long open = all.stream().filter(i -> i.getStatus().isOpen()).count();
        long criticalOpen = all.stream().filter(i -> i.getStatus().isOpen() && i.getPriority() == Priority.P1).count();
        long violations = all.stream().filter(i -> i.getSlaRecord().getStatus() == SlaStatus.BREACHED).count();

        List<Incident> finishedWithSla = all.stream()
                .filter(i -> i.getSlaRecord().getStatus() == SlaStatus.MET
                        || (!i.getStatus().isOpen() && i.getSlaRecord().getStatus() == SlaStatus.BREACHED))
                .toList();
        Integer compliance = finishedWithSla.isEmpty() ? null : (int) Math.round(100.0
                * finishedWithSla.stream().filter(i -> i.getSlaRecord().getStatus() == SlaStatus.MET).count()
                / finishedWithSla.size());

        OptionalDouble avg = all.stream()
                .filter(i -> i.getSlaRecord().getResolvedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getSlaRecord().getResolvedAt()).toMinutes())
                .average();

        List<Incident> atRisk = all.stream()
                .filter(i -> i.getStatus().isOpen() && (i.getSlaRecord().getStatus() == SlaStatus.AT_RISK
                        || i.getSlaRecord().getStatus() == SlaStatus.BREACHED))
                .sorted(Comparator.comparing(i -> i.getSlaRecord().getResolutionDue()))
                .toList();
        List<Incident> unassigned = all.stream()
                .filter(i -> i.getStatus() == IncidentStatus.NEW || i.getStatus() == IncidentStatus.CLASSIFIED)
                .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                .toList();

        Map<String, Long> hotspots = countBy(all.stream()
                .filter(i -> i.getStatus().isOpen() && i.getCreatedAt().isAfter(now.minus(Duration.ofHours(1))))
                .toList(), i -> i.getService().getName());
        hotspots.values().removeIf(count -> count < HOTSPOT_THRESHOLD);

        return new DashboardReport(all.size(), open, criticalOpen, all.size() - open, violations, compliance,
                avg.isPresent() ? Math.round(avg.getAsDouble()) : null,
                countBy(all, i -> i.getService().getName()),
                countBy(all, i -> i.getCategory() == null ? "(unclassified)" : i.getCategory().getName()),
                byPriority(all),
                countBy(all, i -> i.getStatus().name()),
                atRisk, unassigned, hotspots);
    }

    /** Groups and counts, then orders by count (highest first) in a LinkedHashMap. */
    static Map<String, Long> countBy(List<Incident> list, Function<Incident, String> key) {
        return list.stream()
                .collect(Collectors.groupingBy(key, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** P1..P4 always in order, including zero counts. */
    private static Map<String, Long> byPriority(List<Incident> list) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Priority p : EnumSet.allOf(Priority.class)) {
            result.put(p.display(), list.stream().filter(i -> i.getPriority() == p).count());
        }
        return result;
    }
}
