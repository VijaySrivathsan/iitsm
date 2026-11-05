package com.iitsm.audit;

import com.iitsm.domain.audit.AuditEntry;
import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.service.IncidentService;
import com.iitsm.support.MutableClock;
import com.iitsm.support.ServiceLayerTest;
import com.iitsm.support.ServiceLayerTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * The audit trail records every step of an incident's life (Observer: AuditListener).
 */
@ServiceLayerTest
class AuditTrailTest {

    @Autowired private IncidentService incidents;
    @Autowired private AuditService audit;
    @Autowired private TestEntityManager em;
    @Autowired private MutableClock clock;

    private Employee emp;
    private Technician tech;
    private Administrator admin;
    private ITService payment;
    private Category application;

    @BeforeEach
    void setUp() {
        clock.set(ServiceLayerTestConfig.START);
        emp = em.persist(new Employee("emp1", "h", "Asha", null, null, null));
        admin = em.persist(new Administrator("admin", "h", "Anita", null, null, true));
        application = em.persist(new Category("Application", null, null));
        tech = new Technician("tech1", "h", "Karan", null, null, 5, 4);
        tech.addSkill(application);
        em.persist(tech);
        payment = em.persist(new ITService("Payment Gateway", null, 5));
    }

    private String lifecycle() {
        String n = incidents.report(emp, payment.getId(), application.getId(),
                new IncidentReport("Payment down", "Cannot pay", Impact.HIGH, Urgency.HIGH, 500)).getNumber().toString();
        clock.advance(Duration.ofMinutes(2));
        incidents.acknowledge(n, tech);
        incidents.startWork(n, tech);
        clock.advance(Duration.ofMinutes(10));
        incidents.resolve(n, "Restarted payment service", null, tech);
        incidents.verify(n, 5, null, emp);
        incidents.close(n, null, emp);
        return n;
    }

    @Test
    void everyStepIsRecordedInOrderWithActorAndStates() {
        String number = lifecycle();

        List<AuditEntry> history = audit.historyOf(number);

        assertThat(history).extracting(AuditEntry::getAction, AuditEntry::getActor,
                        AuditEntry::getPreviousStatus, AuditEntry::getNewStatus)
                .containsExactly(
                        tuple("CREATED", "emp1", null, IncidentStatus.NEW),
                        tuple("CLASSIFIED", "system", IncidentStatus.NEW, IncidentStatus.CLASSIFIED),
                        tuple("ASSIGNED", "system", IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED),
                        tuple("ACKNOWLEDGED", "tech1", IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED),
                        tuple("WORK_STARTED", "tech1", IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS),
                        tuple("RESOLVED", "tech1", IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED),
                        tuple("VERIFIED", "emp1", IncidentStatus.RESOLVED, IncidentStatus.VERIFIED),
                        tuple("CLOSED", "emp1", IncidentStatus.VERIFIED, IncidentStatus.CLOSED));
        assertThat(history.get(1).getDescription()).contains("P1");
        assertThat(history.get(3).getTimestamp()).isEqualTo(ServiceLayerTestConfig.START.plus(Duration.ofMinutes(2)));
    }

    @Test
    void adminCanFilterTheSystemWideLog() {
        String number = lifecycle();
        assertThat(audit.recent("tech1", null, null, admin))
                .extracting(AuditEntry::getAction)
                .containsExactlyInAnyOrder("ACKNOWLEDGED", "WORK_STARTED", "RESOLVED");
        assertThat(audit.recent(null, "CLOSED", number, admin)).hasSize(1);
    }

    @Test
    void nonAdminCannotReadTheSystemWideLog() {
        assertThatThrownBy(() -> audit.recent(null, null, null, emp))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
}
