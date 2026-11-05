package com.iitsm.intelligence;

import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.Employee;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.DuplicateIncidentException;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.service.IncidentService;
import com.iitsm.support.MutableClock;
import com.iitsm.support.ServiceLayerTest;
import com.iitsm.support.ServiceLayerTestConfig;
import com.iitsm.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.event.ApplicationEvents;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Intelligent features A (duplicates) and B (correlation) with the brief's payment example.
 */
@ServiceLayerTest
class DuplicateDetectionTest {

    @Autowired private IncidentService incidents;
    @Autowired private DuplicateDetectionService duplicates;
    @Autowired private TestEntityManager em;
    @Autowired private ApplicationEvents events;
    @Autowired private MutableClock clock;

    private TestData d;

    @BeforeEach
    void setUp() {
        clock.set(ServiceLayerTestConfig.START);
        d = new TestData(em);
    }

    private Incident report(Employee who, String title, String description) {
        return incidents.report(who, d.payment.getId(), d.application.getId(),
                new IncidentReport(title, description, Impact.HIGH, Urgency.HIGH, 100));
    }

    @Test
    void similarReportsFromOtherEmployees_areFlaggedAgainstTheOriginal() {
        Incident first = report(d.emp1, "Payment application unavailable",
                "The payment application is unavailable and users cannot complete transactions.");
        clock.advance(Duration.ofMinutes(2));
        Incident second = report(d.emp2, "Payment transaction failing", "Customers get an error when paying at checkout");
        clock.advance(Duration.ofMinutes(2));
        Incident third = report(d.emp3, "Unable to make payment", "Payment page shows an error");

        assertThat(first.getPossibleDuplicateOf()).isNull();
        assertThat(second.getPossibleDuplicateOf()).isEqualTo(first);
        assertThat(second.getSimilarityScore()).isGreaterThanOrEqualTo(0.5);
        assertThat(third.getPossibleDuplicateOf()).isEqualTo(first); // points at the original, not the copy
        assertThat(events.stream(IncidentEvent.class).filter(e -> e.type() == IncidentEventType.DUPLICATE_DETECTED))
                .hasSize(2);
        // correlation: all three are listed together for the payment service
        assertThat(duplicates.relatedOnSameService(first)).containsExactly(third, second);
    }

    @Test
    void unrelatedReport_isNotFlagged() {
        report(d.emp1, "Payment application unavailable", "Users cannot pay");
        Incident vpn = incidents.report(d.emp2, d.vpn.getId(), d.network.getId(),
                new IncidentReport("VPN keeps disconnecting", "Drops every few minutes", Impact.MEDIUM, Urgency.MEDIUM, 5));
        assertThat(vpn.getPossibleDuplicateOf()).isNull();
    }

    @Test
    void sameEmployeeRepeatingTheSameReport_isRefused() {
        report(d.emp1, "Payment application unavailable", "Users cannot pay");
        clock.advance(Duration.ofMinutes(10));
        assertThatThrownBy(() -> report(d.emp1, "Payment application unavailable", "Users cannot pay"))
                .isInstanceOf(DuplicateIncidentException.class)
                .hasMessageContaining("INC1001");
    }

    @Test
    void sameEmployeeMayReportAgainAfterAnHour() {
        report(d.emp1, "Payment application unavailable", "Users cannot pay");
        clock.advance(Duration.ofMinutes(61));
        assertThat(report(d.emp1, "Payment application unavailable", "Users cannot pay").getNumber())
                .hasToString("INC1002");
    }

    @Test
    void assignedTechnicianConfirms_employeeMayNot() {
        Incident first = report(d.emp1, "Payment application unavailable", "Users cannot pay");
        Incident second = report(d.emp2, "Payment transaction failing", "Error when paying");
        String n = second.getNumber().toString();

        assertThatThrownBy(() -> incidents.confirmDuplicate(n, d.emp2)).isInstanceOf(UnauthorizedOperationException.class);
        incidents.confirmDuplicate(n, d.tech1); // tech1 was auto-assigned (application skill)

        assertThat(second.getDuplicateOf()).isEqualTo(first);
        assertThat(duplicates.confirmedDuplicatesOf(first)).containsExactly(second);
    }
}
