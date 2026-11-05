package com.iitsm.bootstrap;

import com.iitsm.assignment.AssignmentService;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.repository.AuditEntryRepository;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.repository.TechnicianRepository;
import com.iitsm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo database is ready for the video: accounts, realistic history, and tech1 free for the demo.
 */
@SpringBootTest
@Transactional
class DemoDataTest {

    @Autowired private UserRepository users;
    @Autowired private IncidentRepository incidents;
    @Autowired private AuditEntryRepository audit;
    @Autowired private TechnicianRepository technicians;
    @Autowired private AssignmentService assignment;
    @Autowired private DemoDataLoader loader;

    @Test
    void seedsAccountsAndHistory() {
        assertThat(users.count()).isEqualTo(9);
        List<Incident> all = incidents.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(25);
        assertThat(all).anyMatch(i -> i.getStatus() == IncidentStatus.CLOSED && i.getSlaRecord().getStatus() == SlaStatus.MET);
        assertThat(all).anyMatch(i -> i.getStatus() == IncidentStatus.CLOSED && i.getSlaRecord().getStatus() == SlaStatus.BREACHED);
        assertThat(all).anyMatch(i -> i.getStatus() == IncidentStatus.IN_PROGRESS && i.getSlaRecord().getStatus() == SlaStatus.BREACHED);
        assertThat(all).allMatch(i -> i.getNumber().sequence() < 1001); // live demo starts at INC1001
        assertThat(audit.findByIncidentNumberOrderByTimestampAsc(all.get(0).getNumber().toString())).isNotEmpty();
    }

    @Test
    void tech1StartsTheDemoWithAnEmptyQueue() {
        assertThat(assignment.activeWorkload(technicians.findByUsernameIgnoreCase("tech1").orElseThrow())).isZero();
    }

    @Test
    void loaderDoesNothingWhenDataExists() {
        long before = incidents.count();
        loader.run(new DefaultApplicationArguments());
        assertThat(incidents.count()).isEqualTo(before);
        assertThat(users.count()).isEqualTo(9);
    }
}
