package com.iitsm.assignment;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.event.IncidentEventPublisher;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.TechnicianUnavailableException;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.exception.WorkloadExceededException;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything about who works on an incident: automatic assignment with the active
 * {@link AssignmentStrategy}, manual (re)assignment, smart recommendations and workload figures.
 *
 * <p>It builds the list of eligible {@link Candidate}s (active, available, below max workload) and
 * leaves the choice to the strategy, so strategies never touch the database.
 */
@Service
@Transactional
public class AssignmentService {

    /** Statuses meaning the technician has finished work on an incident (used for service history). */
    private static final Set<IncidentStatus> HANDLED = EnumSet.of(
            IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);

    private final AssignmentStrategyRegistry registry;
    private final SmartRecommendationStrategy smartStrategy;
    private final TechnicianRepository technicians;
    private final IncidentRepository incidents;
    private final IncidentEventPublisher events;
    private final boolean autoAssignEnabled;

    public AssignmentService(AssignmentStrategyRegistry registry, SmartRecommendationStrategy smartStrategy,
                             TechnicianRepository technicians, IncidentRepository incidents,
                             IncidentEventPublisher events,
                             @Value("${itsm.assignment.auto:true}") boolean autoAssignEnabled) {
        this.registry = registry;
        this.smartStrategy = smartStrategy;
        this.technicians = technicians;
        this.incidents = incidents;
        this.events = events;
        this.autoAssignEnabled = autoAssignEnabled;
    }

    // ------------------------------------------------------------------ automatic assignment

    /**
     * Assigns a freshly classified incident using the active strategy. If nobody suitable is free the
     * incident stays CLASSIFIED and a NO_TECHNICIAN_AVAILABLE event lets administrators know.
     *
     * @return the chosen technician, if any
     */
    public Optional<Technician> autoAssign(Incident incident) {
        if (!autoAssignEnabled || incident.getStatus() != IncidentStatus.CLASSIFIED) {
            return Optional.empty();
        }
        AssignmentStrategy strategy = registry.active();
        Optional<Technician> chosen = strategy.select(incident, eligibleCandidates(incident));
        if (chosen.isEmpty()) {
            events.publish(IncidentEventType.NO_TECHNICIAN_AVAILABLE, incident, incident.getStatus(), null,
                    "Strategy '" + strategy.name() + "' found no available technician");
            return Optional.empty();
        }
        IncidentStatus before = incident.getStatus();
        incident.assignTo(chosen.get());
        strategy.recordAssignment(incident, chosen.get());
        events.publish(IncidentEventType.ASSIGNED, incident, before, null,
                "Auto-assigned to " + chosen.get().getUsername() + " by '" + strategy.name() + "' strategy");
        return chosen;
    }

    // ------------------------------------------------------------------ manual assignment

    /** Assigns or reassigns a specific technician (administrator action, or escalation when actor is null). */
    public void assign(Incident incident, Technician technician, User actor) {
        checkCanTakeIncident(incident, technician);
        IncidentStatus before = incident.getStatus();
        incident.assignTo(technician);
        events.publish(IncidentEventType.ASSIGNED, incident, before, actor,
                "Assigned to " + technician.getUsername());
    }

    /** Rejects technicians who are unavailable or already at their maximum workload. */
    public void checkCanTakeIncident(Incident incident, Technician technician) {
        if (!technician.isActive() || !technician.isAvailable()) {
            throw new TechnicianUnavailableException("Technician " + technician.getUsername() + " is not available");
        }
        if (incident.isAssignedTo(technician)) {
            return; // re-assigning to the same person does not add to their workload
        }
        long active = activeWorkload(technician);
        if (!technician.hasCapacityFor((int) active)) {
            throw new WorkloadExceededException(technician.getUsername(), (int) active, technician.getMaxWorkload());
        }
    }

    // ------------------------------------------------------------------ recommendations & workload

    /** Smart recommendation (feature D): every eligible technician scored, with reasons, best first. */
    @Transactional(readOnly = true)
    public List<TechnicianScore> recommend(Incident incident) {
        return smartStrategy.rank(incident, eligibleCandidates(incident));
    }

    /** Active, available technicians below their maximum workload, with the figures strategies need. */
    @Transactional(readOnly = true)
    public List<Candidate> eligibleCandidates(Incident incident) {
        return technicians.findByActiveTrue().stream()
                .filter(Technician::isAvailable)
                .map(t -> new Candidate(t, (int) activeWorkload(t),
                        (int) incidents.countByAssigneeAndServiceAndStatusIn(t, incident.getService(), HANDLED)))
                .filter(c -> c.technician().hasCapacityFor(c.activeIncidents()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long activeWorkload(Technician technician) {
        return incidents.countByAssigneeAndStatusIn(technician, IncidentStatus.workloadStatuses());
    }

    /** Workload of every active technician, busiest first. */
    @Transactional(readOnly = true)
    public List<TechnicianWorkload> workloads() {
        return technicians.findByActiveTrue().stream()
                .map(t -> new TechnicianWorkload(t, activeWorkload(t)))
                .sorted(Comparator.comparingLong(TechnicianWorkload::activeIncidents).reversed()
                        .thenComparing(w -> w.technician().getUsername()))
                .toList();
    }

    // ------------------------------------------------------------------ strategy configuration

    @Transactional(readOnly = true)
    public AssignmentStrategy activeStrategy() {
        return registry.active();
    }

    public List<AssignmentStrategy> availableStrategies() {
        return List.copyOf(registry.all());
    }

    /** An administrator switches the strategy used for automatic assignment. */
    public AssignmentStrategy changeStrategy(String name, User actor) {
        if (!(actor instanceof Administrator)) {
            throw new UnauthorizedOperationException("Only administrators can change the assignment strategy");
        }
        return registry.activate(name);
    }
}
