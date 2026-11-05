package com.iitsm.event;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * The single place that turns "something happened to this incident" into an {@link IncidentEvent}
 * and hands it to Spring's event bus. Every service that changes incidents uses this, so events are
 * always built the same way (number, statuses, actor, time from the shared clock).
 */
@Component
public class IncidentEventPublisher {

    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public IncidentEventPublisher(ApplicationEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * @param before status before the change (null for creation)
     * @param actor  who did it; null means the system
     */
    public IncidentEvent publish(IncidentEventType type, Incident incident, IncidentStatus before, User actor,
                                 String description) {
        IncidentEvent event = new IncidentEvent(type, incident.getId(), incident.getNumber().toString(), before,
                incident.getStatus(), actor == null ? null : actor.getUsername(), description, Instant.now(clock));
        publisher.publishEvent(event);
        return event;
    }
}
