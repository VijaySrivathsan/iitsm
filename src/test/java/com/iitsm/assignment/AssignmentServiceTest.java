package com.iitsm.assignment;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.InvalidInputException;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.repository.SystemSettingRepository;
import com.iitsm.service.IncidentService;
import com.iitsm.sla.SlaService;
import com.iitsm.support.ServiceLayerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.event.ApplicationEvents;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Automatic and manual assignment end-to-end (H2 database, all four strategies registered).
 */
@ServiceLayerTest
class AssignmentServiceTest {

    @Autowired private AssignmentService assignment;
    @Autowired private IncidentService incidents;
    @Autowired private SlaService slaService;
    @Autowired private SystemSettingRepository settings;
    @Autowired private TestEntityManager em;
    @Autowired private ApplicationEvents events;

    private Employee emp;
    private Administrator admin;
    private Technician dbExpert;   // database skill, idle, max 2
    private Technician netTech;    // network skill, idle
    private ITService coreDb;
    private Category database;

    @BeforeEach
    void setUp() {
        slaService.ensureDefaultPolicies();
        emp = em.persist(new Employee("emp1", "h", "Asha", null, null, null));
        admin = em.persist(new Administrator("admin", "h", "Anita", null, null, true));
        database = em.persist(new Category("Database", null, null));
        Category network = em.persist(new Category("Network", null, null));
        dbExpert = new Technician("tech1", "h", "Karan", null, null, 2, 4);
        dbExpert.addSkill(database);
        em.persist(dbExpert);
        netTech = new Technician("tech2", "h", "Divya", null, null, 5, 3);
        netTech.addSkill(network);
        em.persist(netTech);
        coreDb = em.persist(new ITService("Core Database", null, 4));
    }

    /** Different database problems each time (identical repeats would be refused as duplicates). */
    private static final String[][] DB_PROBLEMS = {
            {"Orders table locked", "Deadlock on orders table"},
            {"Replica lag growing", "Read replica is hours behind"},
            {"Backup job failed", "Nightly backup did not finish"},
            {"Reports query timing out", "Monthly report never loads"}};
    private int reported;

    private Incident reportDbIncident() {
        String[] p = DB_PROBLEMS[reported++ % DB_PROBLEMS.length];
        return incidents.report(emp, coreDb.getId(), database.getId(),
                new IncidentReport(p[0], p[1], Impact.MEDIUM, Urgency.HIGH, 20));
    }

    @Test
    void report_withCategory_isAutoAssignedToSkilledTechnician() {
        Incident incident = reportDbIncident();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(incident.getAssignee()).isEqualTo(dbExpert);
        assertThat(events.stream(IncidentEvent.class)).extracting(IncidentEvent::type).containsExactly(
                IncidentEventType.CREATED, IncidentEventType.CLASSIFIED, IncidentEventType.ASSIGNED);
        assertThat(events.stream(IncidentEvent.class).toList().get(2).description()).contains("'skill' strategy");
    }

    @Test
    void whenSkilledTechnicianIsFull_incidentStaysClassifiedAndAdminsAreAlerted() {
        reportDbIncident();
        reportDbIncident(); // tech1 (max 2) is now full
        Incident third = reportDbIncident();

        assertThat(third.getStatus()).isEqualTo(IncidentStatus.CLASSIFIED);
        assertThat(third.getAssignee()).isNull();
        assertThat(events.stream(IncidentEvent.class)).extracting(IncidentEvent::type)
                .endsWith(IncidentEventType.NO_TECHNICIAN_AVAILABLE);
    }

    @Test
    void switchingStrategy_changesWhoGetsTheNextIncident() {
        reportDbIncident(); // tech1 now has 1 active incident, tech2 has 0

        assignment.changeStrategy(LoadBasedAssignmentStrategy.NAME, admin);
        Incident next = reportDbIncident();

        assertThat(next.getAssignee()).isEqualTo(netTech); // least busy, skill ignored
        assertThat(settings.findById(AssignmentStrategyRegistry.SETTING_KEY))
                .get().extracting(s -> s.getValue()).isEqualTo("load");
        assertThat(assignment.activeStrategy().name()).isEqualTo("load");
    }

    @Test
    void changeStrategy_validatesNameAndRole() {
        assertThatThrownBy(() -> assignment.changeStrategy("magic", admin))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("smart");
        assertThatThrownBy(() -> assignment.changeStrategy("load", emp))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void unavailableTechnicians_areNeverCandidates() {
        dbExpert.markUnavailable();
        Incident incident = reportDbIncident();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLASSIFIED);
        assertThat(assignment.eligibleCandidates(incident)).extracting(Candidate::technician).containsExactly(netTech);
    }

    @Test
    void recommend_ranksAllEligibleTechniciansWithReasons() {
        Incident incident = reportDbIncident();
        List<TechnicianScore> ranking = assignment.recommend(incident);
        assertThat(ranking).extracting(s -> s.technician().getUsername()).containsExactly("tech1", "tech2");
        assertThat(ranking.get(0).reason()).contains("skill 40");
    }

    @Test
    void workloads_listsBusiestFirst() {
        reportDbIncident();
        assertThat(assignment.workloads())
                .extracting(w -> w.technician().getUsername(), TechnicianWorkload::activeIncidents)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("tech1", 1L),
                        org.assertj.core.groups.Tuple.tuple("tech2", 0L));
    }
}
