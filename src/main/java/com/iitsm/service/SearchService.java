package com.iitsm.service;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.iitsm.repository.IncidentSpecifications.assignedTo;
import static com.iitsm.repository.IncidentSpecifications.assignedToId;
import static com.iitsm.repository.IncidentSpecifications.createdBefore;
import static com.iitsm.repository.IncidentSpecifications.createdFrom;
import static com.iitsm.repository.IncidentSpecifications.hasNumber;
import static com.iitsm.repository.IncidentSpecifications.hasPriority;
import static com.iitsm.repository.IncidentSpecifications.hasSlaStatus;
import static com.iitsm.repository.IncidentSpecifications.hasStatus;
import static com.iitsm.repository.IncidentSpecifications.inCategory;
import static com.iitsm.repository.IncidentSpecifications.onService;
import static com.iitsm.repository.IncidentSpecifications.reportedBy;
import static com.iitsm.repository.IncidentSpecifications.textContains;

/**
 * Multi-criteria incident search. Results are always limited to what the user may see: administrators
 * and team leads see everything, technicians their assigned incidents, employees their own.
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private final IncidentRepository incidents;
    private final ZoneId zone;

    public SearchService(IncidentRepository incidents, @Value("${itsm.timezone:Asia/Kolkata}") String zone) {
        this.incidents = incidents;
        this.zone = ZoneId.of(zone);
    }

    public List<Incident> search(IncidentSearchCriteria c, User viewer) {
        Specification<Incident> spec = Specification.allOf(
                visibleTo(viewer),
                hasNumber(c.number()),
                textContains(c.text()),
                createdFrom(startOf(c.from())),
                createdBefore(startOf(c.to() == null ? null : c.to().plusDays(1))),
                hasStatus(c.status()),
                hasPriority(c.priority()),
                inCategory(c.categoryId()),
                onService(c.serviceId()),
                assignedToId(c.technicianId()),
                hasSlaStatus(c.slaStatus()));
        return incidents.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Specification<Incident> visibleTo(User viewer) {
        if (viewer instanceof Administrator) {
            return null;
        }
        if (viewer instanceof Technician tech) {
            return tech.isTeamLead() ? null : assignedTo(tech);
        }
        return reportedBy((Employee) viewer);
    }

    private Instant startOf(LocalDate day) {
        return day == null ? null : day.atStartOfDay(zone).toInstant();
    }
}
