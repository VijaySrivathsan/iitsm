package com.iitsm.repository;

import com.iitsm.domain.audit.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for the audit trail.
 */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    List<AuditEntry> findByIncidentNumberOrderByTimestampAsc(String incidentNumber);

    List<AuditEntry> findTop200ByOrderByTimestampDesc();
}
