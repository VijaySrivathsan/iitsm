package com.iitsm.audit;

import com.iitsm.domain.audit.AuditEntry;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.User;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.repository.AuditEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Read access to the audit trail: one incident's history, and the recent system-wide log for
 * administrators with simple filters.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private final AuditEntryRepository auditEntries;

    public AuditService(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    /** Chronological history of one incident. The caller must already be allowed to view it. */
    public List<AuditEntry> historyOf(String incidentNumber) {
        return auditEntries.findByIncidentNumberOrderByTimestampAsc(incidentNumber.toUpperCase(Locale.ROOT));
    }

    /**
     * Most recent audit lines (newest first), optionally filtered by actor, action and incident
     * number (each filter is ignored when blank). Administrators only.
     */
    public List<AuditEntry> recent(String actor, String action, String incidentNumber, User viewer) {
        if (!(viewer instanceof Administrator)) {
            throw new UnauthorizedOperationException("Only administrators can view the audit log");
        }
        return auditEntries.findTop200ByOrderByTimestampDesc().stream()
                .filter(e -> blank(actor) || e.getActor().equalsIgnoreCase(actor.trim()))
                .filter(e -> blank(action) || e.getAction().equalsIgnoreCase(action.trim()))
                .filter(e -> blank(incidentNumber) || incidentNumber.trim().equalsIgnoreCase(e.getIncidentNumber()))
                .toList();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
