package com.iitsm.repository;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for incidents. {@link JpaSpecificationExecutor} enables the multi-criteria search
 * built in Milestone 8.
 */
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findBySequenceNo(long sequenceNo);

    /** Highest incident number used so far, or null if there are no incidents. */
    @Query("select max(i.sequenceNo) from Incident i")
    Long findMaxSequenceNo();

    List<Incident> findByReporterOrderByCreatedAtDesc(Employee reporter);

    List<Incident> findByAssigneeOrderByCreatedAtDesc(Technician assignee);

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);

    /** Number of incidents in the given statuses held by a technician (their workload). */
    long countByAssigneeAndStatusIn(Technician assignee, Collection<IncidentStatus> statuses);

    /** Open incidents reported since a moment (used by duplicate detection and correlation). */
    List<Incident> findByStatusInAndCreatedAtAfter(Collection<IncidentStatus> statuses, java.time.Instant since);

    /** Open incidents in the given statuses (used by the SLA monitor). */
    List<Incident> findByStatusInOrderByCreatedAtAsc(Collection<IncidentStatus> statuses);

    List<Incident> findByDuplicateOf(Incident parent);

    /** How many incidents on a service this technician has handled (experience with that service). */
    long countByAssigneeAndServiceAndStatusIn(Technician assignee, ITService service,
                                              Collection<IncidentStatus> statuses);
}
