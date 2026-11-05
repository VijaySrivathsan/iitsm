package com.iitsm.service;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentFactory;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.IncidentNotFoundException;
import com.iitsm.exception.InvalidStateTransitionException;
import com.iitsm.exception.TechnicianUnavailableException;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.exception.WorkloadExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IncidentService against a real (H2) database: authorisation rules, persistence and the events
 * published for the Observer pattern.
 */
@DataJpaTest
@Import({IncidentService.class, IncidentFactory.class, IncidentServiceTest.FixedClock.class})
@RecordApplicationEvents
class IncidentServiceTest {

    static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private IncidentService service;
    @Autowired private TestEntityManager em;
    @Autowired private ApplicationEvents events;

    private Employee emp1;
    private Employee emp2;
    private Technician tech1;
    private Technician tech2;
    private Administrator admin;
    private ITService payment;
    private Category application;

    @BeforeEach
    void setUp() {
        emp1 = em.persist(new Employee("emp1", "h", "Asha Rao", null, null, "Finance"));
        emp2 = em.persist(new Employee("emp2", "h", "Rahul Mehta", null, null, "Sales"));
        tech1 = em.persist(new Technician("tech1", "h", "Karan Shah", null, null, 1, 4));
        tech2 = em.persist(new Technician("tech2", "h", "Divya Iyer", null, null, 5, 3));
        admin = em.persist(new Administrator("admin", "h", "Anita Menon", null, null, true));
        payment = em.persist(new ITService("Payment Gateway", null, 5));
        application = em.persist(new Category("Application", null, null));
    }

    private Incident reportPaymentOutage(Employee reporter) {
        return service.report(reporter, payment.getId(), new IncidentReport("Payment app down",
                "Users cannot complete transactions", Impact.HIGH, Urgency.HIGH, 500));
    }

    private String classifiedAndAssignedTo(Technician tech) {
        String number = reportPaymentOutage(emp1).getNumber().toString();
        service.classify(number, application.getId(), Priority.P1, null, admin);
        service.assign(number, tech.getId(), admin);
        return number;
    }

    @Test
    void report_byEmployee_createsNumberedNewIncidentAndPublishesEvent() {
        Incident first = reportPaymentOutage(emp1);
        Incident second = reportPaymentOutage(emp2);

        assertThat(first.getNumber()).hasToString("INC1001");
        assertThat(second.getNumber()).hasToString("INC1002");
        assertThat(first.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(first.getCreatedAt()).isEqualTo(NOW);
        assertThat(events.stream(IncidentEvent.class))
                .extracting(IncidentEvent::type, IncidentEvent::actor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(IncidentEventType.CREATED, "emp1"),
                        org.assertj.core.groups.Tuple.tuple(IncidentEventType.CREATED, "emp2"));
    }

    @Test
    void report_byTechnician_isUnauthorized() {
        assertThatThrownBy(() -> service.report(tech1, payment.getId(),
                new IncidentReport("x", "y", null, null, null)))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void fullLifecycle_publishesOneEventPerStep() {
        String number = classifiedAndAssignedTo(tech2);
        service.acknowledge(number, tech2);
        service.startWork(number, tech2);
        service.requestInfo(number, "Which page fails?", tech2);
        service.provideInfo(number, "Checkout page", emp1);
        service.resolve(number, "Restarted payment service", "Memory leak", tech2);
        service.verify(number, 5, "Thanks", emp1);
        Incident closed = service.close(number, "Confirmed by user", emp1);

        assertThat(closed.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(events.stream(IncidentEvent.class)).extracting(IncidentEvent::type).containsExactly(
                IncidentEventType.CREATED, IncidentEventType.CLASSIFIED, IncidentEventType.ASSIGNED,
                IncidentEventType.ACKNOWLEDGED, IncidentEventType.WORK_STARTED, IncidentEventType.INFO_REQUESTED,
                IncidentEventType.INFO_PROVIDED, IncidentEventType.RESOLVED, IncidentEventType.VERIFIED,
                IncidentEventType.CLOSED);
        IncidentEvent resolved = events.stream(IncidentEvent.class)
                .filter(e -> e.type() == IncidentEventType.RESOLVED).findFirst().orElseThrow();
        assertThat(resolved.previousStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(resolved.newStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }

    @Test
    void employee_cannotResolve() {
        String number = classifiedAndAssignedTo(tech2);
        assertThatThrownBy(() -> service.resolve(number, "fix", null, emp1))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void otherTechnician_cannotAcknowledge() {
        String number = classifiedAndAssignedTo(tech2);
        assertThatThrownBy(() -> service.acknowledge(number, tech1))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void employee_cannotViewSomeoneElsesIncident_butAdminCan() {
        String number = reportPaymentOutage(emp1).getNumber().toString();
        assertThatThrownBy(() -> service.view(number, emp2)).isInstanceOf(UnauthorizedOperationException.class);
        assertThat(service.view(number, admin).getNumber()).hasToString(number);
        assertThat(service.view(number, emp1).getNumber()).hasToString(number);
    }

    @Test
    void closingANewIncident_isAnInvalidTransition() {
        String number = reportPaymentOutage(emp1).getNumber().toString();
        assertThatThrownBy(() -> service.close(number, null, admin))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void assign_toUnavailableTechnician_isRejected() {
        tech2.markUnavailable();
        String number = reportPaymentOutage(emp1).getNumber().toString();
        service.classify(number, application.getId(), Priority.P1, null, admin);
        assertThatThrownBy(() -> service.assign(number, tech2.getId(), admin))
                .isInstanceOf(TechnicianUnavailableException.class);
    }

    @Test
    void assign_beyondMaxWorkload_isRejected() {
        classifiedAndAssignedTo(tech1); // tech1 max workload is 1, now full
        String second = reportPaymentOutage(emp2).getNumber().toString();
        service.classify(second, application.getId(), Priority.P2, null, admin);
        assertThatThrownBy(() -> service.assign(second, tech1.getId(), admin))
                .isInstanceOf(WorkloadExceededException.class);
    }

    @Test
    void unknownIncidentNumber_isNotFound() {
        assertThatThrownBy(() -> service.view("INC9999", admin)).isInstanceOf(IncidentNotFoundException.class);
    }
}
