package com.iitsm.intelligence;

import com.iitsm.domain.escalation.Escalation;
import com.iitsm.domain.escalation.EscalationLevel;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.sla.SlaRecord;
import com.iitsm.domain.user.User;
import com.iitsm.event.IncidentEventPublisher;
import com.iitsm.event.IncidentEventType;
import com.iitsm.repository.AdministratorRepository;
import com.iitsm.repository.EscalationRepository;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Intelligent feature C - automatic escalation. A major (P1/P2) incident that nobody has acknowledged
 * by its response deadline is escalated one level at a time: team lead, then system administrator,
 * then IT manager - one more response window apart. Acknowledging the incident stops the chain
 * (it is no longer CLASSIFIED/ASSIGNED).
 *
 * <p>The chain is a {@link Deque}: levels already used are removed from the front, and the next level
 * is whatever is left at the head.
 */
@Service
@Transactional
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private static final EnumSet<IncidentStatus> UNACKNOWLEDGED =
            EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED);

    private final IncidentRepository incidents;
    private final EscalationRepository escalations;
    private final TechnicianRepository technicians;
    private final AdministratorRepository administrators;
    private final IncidentEventPublisher events;
    private final boolean enabled;

    public EscalationService(IncidentRepository incidents, EscalationRepository escalations,
                             TechnicianRepository technicians, AdministratorRepository administrators,
                             IncidentEventPublisher events, @Value("${itsm.escalation.enabled:true}") boolean enabled) {
        this.incidents = incidents;
        this.escalations = escalations;
        this.technicians = technicians;
        this.administrators = administrators;
        this.events = events;
        this.enabled = enabled;
    }

    /** Escalates every overdue unacknowledged major incident by (at most) one level. */
    public List<Escalation> escalateOverdue(Instant now) {
        List<Escalation> created = new ArrayList<>();
        if (!enabled) {
            return created;
        }
        for (Incident incident : incidents.findByStatusInOrderByCreatedAtAsc(UNACKNOWLEDGED)) {
            escalateIfDue(incident, now).ifPresent(created::add);
        }
        return created;
    }

    Optional<Escalation> escalateIfDue(Incident incident, Instant now) {
        SlaRecord sla = incident.getSlaRecord();
        if (incident.getPriority() == null || !incident.getPriority().isMajor() || !sla.isStarted()
                || sla.getRespondedAt() != null) {
            return Optional.empty();
        }
        long done = escalations.countByIncident(incident);
        Deque<EscalationLevel> chain = new ArrayDeque<>(Arrays.asList(EscalationLevel.values()));
        for (long i = 0; i < done && !chain.isEmpty(); i++) {
            chain.pollFirst();
        }
        if (chain.isEmpty()) {
            return Optional.empty(); // already at the top of the chain
        }
        Duration window = Duration.between(incident.getCreatedAt(), sla.getResponseDue());
        Instant dueAt = sla.getResponseDue().plus(window.multipliedBy(done));
        if (!now.isAfter(dueAt)) {
            return Optional.empty();
        }
        EscalationLevel level = chain.peekFirst();
        Optional<User> target = targetFor(level, incident);
        if (target.isEmpty()) {
            log.warn("No one to escalate {} to at level {}", incident.getNumber(), level);
            return Optional.empty();
        }
        long minutes = Duration.between(incident.getCreatedAt(), now).toMinutes();
        String reason = incident.getPriority() + " not acknowledged " + minutes + " min after it was reported";
        Escalation escalation = escalations.save(new Escalation(incident, level, target.get(), now, reason));
        events.publish(IncidentEventType.ESCALATED, incident, incident.getStatus(), null,
                "Level " + level.step() + " (" + level.label() + "): escalated to " + target.get().getUsername()
                        + " - " + reason);
        return Optional.of(escalation);
    }

    private Optional<User> targetFor(EscalationLevel level, Incident incident) {
        return switch (level) {
            case TEAM_LEAD -> technicians.findByTeamLeadTrueAndActiveTrue().stream()
                    .filter(lead -> !incident.isAssignedTo(lead))
                    .<User>map(lead -> lead)
                    .findFirst();
            case ADMINISTRATOR -> administrators.findByActiveTrue().stream()
                    .sorted((a, b) -> Boolean.compare(a.isItManager(), b.isItManager())) // prefer non-manager
                    .<User>map(a -> a)
                    .findFirst();
            case IT_MANAGER -> administrators.findByItManagerTrueAndActiveTrue().stream()
                    .<User>map(a -> a)
                    .findFirst()
                    .or(() -> administrators.findByActiveTrue().stream().<User>map(a -> a).findFirst());
        };
    }

    @Transactional(readOnly = true)
    public List<Escalation> historyOf(Incident incident) {
        return escalations.findByIncidentOrderByEscalatedAtAsc(incident);
    }
}
