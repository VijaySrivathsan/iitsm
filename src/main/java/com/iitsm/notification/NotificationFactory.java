package com.iitsm.notification;

import com.iitsm.domain.incident.Incident;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Factory pattern: builds the {@link NotificationMessage} for an event, so wording lives in one
 * place and every channel sends the same content. Also decides whether a message is urgent.
 */
@Component
public class NotificationFactory {

    private static final Set<IncidentEventType> ALWAYS_URGENT = EnumSet.of(
            IncidentEventType.SLA_APPROACHING, IncidentEventType.SLA_VIOLATED,
            IncidentEventType.ESCALATED, IncidentEventType.NO_TECHNICIAN_AVAILABLE);

    public NotificationMessage create(IncidentEvent event, Incident incident) {
        String number = event.incidentNumber();
        String title = incident.getTitle();
        String priority = incident.getPriority() == null ? "not yet prioritised" : incident.getPriority().display();
        String subject = switch (event.type()) {
            case CREATED -> number + " received: " + title;
            case CLASSIFIED -> number + " classified as " + priority;
            case ASSIGNED -> number + " assigned to " + assigneeName(incident) + " (" + priority + ")";
            case INFO_REQUESTED -> number + ": the technician needs more information";
            case RESOLVED -> number + " has been resolved - please verify";
            case SLA_APPROACHING -> number + ": SLA deadline approaching (" + priority + ")";
            case SLA_VIOLATED -> number + ": SLA VIOLATED (" + priority + ")";
            case ESCALATED -> number + " escalated: " + title;
            case NO_TECHNICIAN_AVAILABLE -> number + ": no technician available - assign manually";
            default -> number + ": " + event.type().label();
        };
        StringBuilder body = new StringBuilder()
                .append(event.type().label()).append(" - ").append(number).append(": ").append(title).append('\n')
                .append("Status: ").append(incident.getStatus()).append(", priority: ").append(priority).append('\n');
        if (event.description() != null) {
            body.append(event.description()).append('\n');
        }
        body.append("By: ").append(event.actor());
        return new NotificationMessage(subject, body.toString(), number, isUrgent(event, incident));
    }

    static boolean isUrgent(IncidentEvent event, Incident incident) {
        if (ALWAYS_URGENT.contains(event.type())) {
            return true;
        }
        return event.type() == IncidentEventType.ASSIGNED
                && incident.getPriority() != null && incident.getPriority().isMajor();
    }

    private static String assigneeName(Incident incident) {
        return incident.getAssignee() == null ? "nobody" : incident.getAssignee().getFullName();
    }
}
