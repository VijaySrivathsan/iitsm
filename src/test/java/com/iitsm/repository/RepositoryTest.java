package com.iitsm.repository;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.NoteType;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests against an in-memory H2 database.
 */
@DataJpaTest
class RepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    @Autowired private TestEntityManager em;
    @Autowired private IncidentRepository incidents;
    @Autowired private TechnicianRepository technicians;
    @Autowired private UserRepository users;
    @Autowired private SlaPolicyRepository slaPolicies;

    private Employee reporter;
    private ITService payment;
    private Category database;
    private Category network;

    @BeforeEach
    void setUp() {
        reporter = em.persist(new Employee("emp1", "hash", "Asha Rao", "asha@example.com", null, "Finance"));
        payment = em.persist(new ITService("Payment Gateway", "Card payments", 5));
        database = em.persist(new Category("Database", null, "sql"));
        network = em.persist(new Category("Network", null, "vpn"));
    }

    @Test
    void incident_withNotes_roundTripsThroughDatabase() {
        Incident incident = new Incident(new IncidentNumber(1001), "Payment app down", "Cannot pay",
                reporter, payment, Impact.HIGH, Urgency.HIGH, 500, NOW);
        incident.addNote(reporter, NoteType.INFO_RESPONSE, "Fails at checkout", NOW.plusSeconds(60));
        incidents.save(incident);
        em.flush();
        em.clear(); // force a real reload from the database

        Incident loaded = incidents.findBySequenceNo(1001).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("Payment app down");
        assertThat(loaded.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(loaded.getReporter().getUsername()).isEqualTo("emp1");
        assertThat(loaded.getService().getName()).isEqualTo("Payment Gateway");
        assertThat(loaded.getNotes()).singleElement()
                .satisfies(n -> assertThat(n.getText()).isEqualTo("Fails at checkout"));
        assertThat(loaded.getSlaRecord()).isNotNull();
    }

    @Test
    void findMaxSequenceNo_givesNextIncidentNumber() {
        assertThat(incidents.findMaxSequenceNo()).isNull();
        incidents.save(new Incident(IncidentNumber.next(null), "a", "a", reporter, payment,
                Impact.LOW, Urgency.LOW, 1, NOW));
        incidents.save(new Incident(new IncidentNumber(1007), "b", "b", reporter, payment,
                Impact.LOW, Urgency.LOW, 1, NOW));
        assertThat(IncidentNumber.next(incidents.findMaxSequenceNo())).hasToString("INC1008");
    }

    @Test
    void technicians_canBeFoundBySkill() {
        Technician dbTech = new Technician("tech1", "hash", "Karan Shah", null, null, 5, 4);
        dbTech.addSkill(database);
        Technician netTech = new Technician("tech2", "hash", "Divya Iyer", null, null, 5, 3);
        netTech.addSkill(network);
        Technician inactiveDbTech = new Technician("tech3", "hash", "Old Tech", null, null, 5, 2);
        inactiveDbTech.addSkill(database);
        inactiveDbTech.deactivate();
        technicians.saveAll(List.of(dbTech, netTech, inactiveDbTech));
        em.flush();
        em.clear();

        List<Technician> found = technicians.findBySkillsContainingAndActiveTrue(database);
        assertThat(found).extracting(Technician::getUsername).containsExactly("tech1");
    }

    @Test
    void userLookup_isPolymorphic() {
        em.persist(new Technician("tech1", "hash", "Karan Shah", null, null, 5, 4));
        em.flush();
        em.clear();

        User user = users.findByUsernameIgnoreCase("TECH1").orElseThrow();
        assertThat(user).isInstanceOf(Technician.class);
        assertThat(users.findByUsernameIgnoreCase("emp1").orElseThrow()).isInstanceOf(Employee.class);
    }

    @Test
    void workloadCount_countsOnlyActiveStatuses() {
        Technician tech = em.persist(new Technician("tech1", "hash", "Karan Shah", null, null, 5, 4));
        long count = incidents.countByAssigneeAndStatusIn(tech, EnumSet.of(IncidentStatus.ASSIGNED,
                IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.PENDING_INFO));
        assertThat(count).isZero();
    }

    @Test
    void slaPolicy_serviceSpecificAndDefaultLookups() {
        slaPolicies.save(SlaPolicy.defaultFor(Priority.P1, 5, 30));
        slaPolicies.save(new SlaPolicy(payment, Priority.P1, 2, 15));
        em.flush();

        assertThat(slaPolicies.findByServiceAndPriority(payment, Priority.P1)).get()
                .extracting(SlaPolicy::getResolutionMinutes).isEqualTo(15);
        assertThat(slaPolicies.findByServiceIsNullAndPriority(Priority.P1)).get()
                .extracting(SlaPolicy::getResolutionMinutes).isEqualTo(30);
        assertThat(slaPolicies.findByServiceIsNullAndPriority(Priority.P4)).isEmpty();
    }
}
