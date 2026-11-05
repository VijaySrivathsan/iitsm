package com.iitsm.audit;

import com.iitsm.domain.audit.AuditEntry;
import com.iitsm.event.IncidentEvent;
import com.iitsm.repository.AuditEntryRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Observer: writes one {@link AuditEntry} for every {@link IncidentEvent}, giving a complete,
 * chronological history (who, what, when, from which status to which).
 *
 * <p>Runs synchronously inside the same transaction as the change, so an incident change and its
 * audit line are saved together or not at all - the trail can never miss a committed change.
 */
@Component
public class AuditListener {

    private final AuditEntryRepository auditEntries;

    public AuditListener(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    @EventListener
    @Order(0) // record the audit line before notifications are attempted
    public void on(IncidentEvent event) {
        String description = event.description() == null
                ? event.type().label()
                : event.type().label() + ": " + event.description();
        auditEntries.save(new AuditEntry(event.occurredAt(), event.actor(), event.type().name(),
                event.incidentNumber(), event.previousStatus(), event.newStatus(), truncate(description)));
    }

    private static String truncate(String text) {
        return text.length() <= 1000 ? text : text.substring(0, 997) + "...";
    }
}
