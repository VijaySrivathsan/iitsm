package com.iitsm.repository;

import com.iitsm.domain.escalation.Escalation;
import com.iitsm.domain.incident.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for escalation records.
 */
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    List<Escalation> findByIncidentOrderByEscalatedAtAsc(Incident incident);

    Optional<Escalation> findTopByIncidentOrderByEscalatedAtDesc(Incident incident);

    long countByIncident(Incident incident);
}
