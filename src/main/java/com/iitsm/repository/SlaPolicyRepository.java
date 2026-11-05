package com.iitsm.repository;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for SLA policies: service-specific ones and the defaults (service = null).
 */
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByServiceAndPriority(ITService service, Priority priority);

    Optional<SlaPolicy> findByServiceIsNullAndPriority(Priority priority);
}
