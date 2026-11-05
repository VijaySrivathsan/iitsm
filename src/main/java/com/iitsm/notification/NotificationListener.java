package com.iitsm.notification;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.user.User;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.ExternalServiceException;
import com.iitsm.repository.AdministratorRepository;
import com.iitsm.repository.EscalationRepository;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.iitsm.notification.Audience.ADMINS;
import static com.iitsm.notification.Audience.ASSIGNEE;
import static com.iitsm.notification.Audience.ESCALATION_TARGET;
import static com.iitsm.notification.Audience.REPORTER;
import static com.iitsm.notification.Audience.TEAM_LEADS;

/**
 * Observer: reacts to every {@link IncidentEvent} by deciding <b>who</b> should hear about it
 * (a rule table per event type) and sending the message through each recipient's channels.
 *
 * <p>The person who caused the event is never notified about their own action. A failing channel
 * (e.g. the SMS provider is down) is logged and skipped - it never blocks the incident update or the
 * other channels.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    /** Who is told about each event type. Events not listed notify nobody. */
    private static final Map<IncidentEventType, Set<Audience>> RULES = new EnumMap<>(IncidentEventType.class);

    static {
        RULES.put(IncidentEventType.CREATED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.CLASSIFIED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.PRIORITY_CHANGED, EnumSet.of(REPORTER, ASSIGNEE));
        RULES.put(IncidentEventType.ASSIGNED, EnumSet.of(REPORTER, ASSIGNEE));
        RULES.put(IncidentEventType.NO_TECHNICIAN_AVAILABLE, EnumSet.of(ADMINS));
        RULES.put(IncidentEventType.ACKNOWLEDGED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.WORK_STARTED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.INFO_REQUESTED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.INFO_PROVIDED, EnumSet.of(ASSIGNEE));
        RULES.put(IncidentEventType.RESOLVED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.VERIFIED, EnumSet.of(ASSIGNEE));
        RULES.put(IncidentEventType.REOPENED, EnumSet.of(ASSIGNEE));
        RULES.put(IncidentEventType.CLOSED, EnumSet.of(REPORTER, ASSIGNEE));
        RULES.put(IncidentEventType.SLA_APPROACHING, EnumSet.of(ASSIGNEE, TEAM_LEADS));
        RULES.put(IncidentEventType.SLA_VIOLATED, EnumSet.of(ASSIGNEE, TEAM_LEADS, ADMINS));
        RULES.put(IncidentEventType.ESCALATED, EnumSet.of(ASSIGNEE, ESCALATION_TARGET));
        RULES.put(IncidentEventType.DUPLICATE_CONFIRMED, EnumSet.of(REPORTER));
        RULES.put(IncidentEventType.DUPLICATE_DETECTED, EnumSet.of(ASSIGNEE));
    }

    private final IncidentRepository incidents;
    private final TechnicianRepository technicians;
    private final AdministratorRepository administrators;
    private final EscalationRepository escalations;
    private final NotificationFactory factory;
    private final NotificationPreferences preferences;
    private final List<NotificationChannel> channels;

    public NotificationListener(IncidentRepository incidents, TechnicianRepository technicians,
                                AdministratorRepository administrators, EscalationRepository escalations,
                                NotificationFactory factory,
                                NotificationPreferences preferences, List<NotificationChannel> channels) {
        this.incidents = incidents;
        this.technicians = technicians;
        this.administrators = administrators;
        this.escalations = escalations;
        this.factory = factory;
        this.preferences = preferences;
        this.channels = channels;
    }

    @EventListener
    @Order(10)
    public void on(IncidentEvent event) {
        Set<Audience> audiences = RULES.get(event.type());
        if (audiences == null || event.incidentId() == null) {
            return;
        }
        Incident incident = incidents.findById(event.incidentId()).orElse(null);
        if (incident == null) {
            return;
        }
        NotificationMessage message = factory.create(event, incident);
        for (User recipient : recipients(audiences, incident, event.actor())) {
            deliver(recipient, message);
        }
    }

    /** Distinct, active recipients for the audiences, excluding whoever caused the event. */
    Set<User> recipients(Set<Audience> audiences, Incident incident, String actor) {
        Set<User> result = new LinkedHashSet<>();
        for (Audience audience : audiences) {
            switch (audience) {
                case REPORTER -> result.add(incident.getReporter());
                case ASSIGNEE -> {
                    if (incident.getAssignee() != null) {
                        result.add(incident.getAssignee());
                    }
                }
                case TEAM_LEADS -> result.addAll(technicians.findByTeamLeadTrueAndActiveTrue());
                case ADMINS -> result.addAll(administrators.findByActiveTrue());
                case ESCALATION_TARGET -> escalations.findTopByIncidentOrderByEscalatedAtDesc(incident)
                        .ifPresent(e -> result.add(e.getEscalatedTo()));
            }
        }
        result.removeIf(u -> !u.isActive() || u.getUsername().equalsIgnoreCase(actor));
        return result;
    }

    private void deliver(User recipient, NotificationMessage message) {
        Set<?> wanted = preferences.channelsFor(recipient.getRole());
        for (NotificationChannel channel : channels) {
            if (!wanted.contains(channel.type()) || !channel.accepts(recipient, message)) {
                continue;
            }
            try {
                channel.send(recipient, message);
            } catch (ExternalServiceException e) {
                log.warn("{} notification to {} failed: {}", channel.type(), recipient.getUsername(), e.getMessage());
            } catch (RuntimeException e) {
                // A broken channel must never stop the incident update or the other channels.
                log.error("{} notification to {} failed unexpectedly", channel.type(), recipient.getUsername(), e);
            }
        }
    }
}
