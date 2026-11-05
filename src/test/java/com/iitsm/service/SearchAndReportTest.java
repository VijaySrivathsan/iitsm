package com.iitsm.service;

import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.support.MutableClock;
import com.iitsm.support.ServiceLayerTest;
import com.iitsm.support.ServiceLayerTestConfig;
import com.iitsm.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-criteria search (9 filters + visibility) and the dashboard report.
 */
@ServiceLayerTest
class SearchAndReportTest {

    @Autowired private IncidentService incidents;
    @Autowired private SearchService search;
    @Autowired private ReportService reports;
    @Autowired private TestEntityManager em;
    @Autowired private MutableClock clock;

    private TestData d;
    private Incident payment;   // emp1, P1, application, assigned to tech1
    private Incident vpn;       // emp2, no category -> NEW
    private Incident hr;        // emp1, P4, application, assigned to tech1, later closed

    @BeforeEach
    void setUp() {
        clock.set(ServiceLayerTestConfig.START);
        d = new TestData(em);
        payment = incidents.report(d.emp1, d.payment.getId(), d.application.getId(),
                new IncidentReport("Payment down", "Cannot pay", Impact.HIGH, Urgency.HIGH, 500));
        vpn = incidents.report(d.emp2, d.vpn.getId(), null,
                new IncidentReport("VPN keeps disconnecting", "Drops often", Impact.MEDIUM, Urgency.MEDIUM, 5));
        hr = incidents.report(d.emp1, d.hrPortal.getId(), d.application.getId(),
                new IncidentReport("HR page typo", "Small typo", Impact.LOW, Urgency.LOW, 1));
        String n = hr.getNumber().toString();
        incidents.acknowledge(n, d.tech1);
        incidents.startWork(n, d.tech1);
        clock.advance(Duration.ofMinutes(30));
        incidents.resolve(n, "Fixed typo", null, d.tech1);
        incidents.verify(n, 4, null, d.emp1);
        incidents.close(n, null, d.emp1);
    }

    private List<Incident> find(Consumer<Builder> filters) {
        Builder b = new Builder();
        filters.accept(b);
        return search.search(b.build(), d.admin);
    }

    /** Small helper to fill only the fields a test needs. */
    static class Builder {
        String number, text; LocalDate from, to; IncidentStatus status; Priority priority;
        Long categoryId, serviceId, technicianId; SlaStatus sla;

        IncidentSearchCriteria build() {
            return new IncidentSearchCriteria(number, text, from, to, status, priority, categoryId, serviceId,
                    technicianId, sla);
        }
    }

    @Test
    void eachFilterWorksOnItsOwn() {
        assertThat(find(b -> b.number = "inc1002")).containsExactly(vpn);
        assertThat(find(b -> b.text = "vpn")).containsExactly(vpn);
        assertThat(find(b -> b.status = IncidentStatus.NEW)).containsExactly(vpn);
        assertThat(find(b -> b.priority = Priority.P1)).containsExactly(payment);
        assertThat(find(b -> b.categoryId = d.application.getId())).containsExactlyInAnyOrder(payment, hr);
        assertThat(find(b -> b.serviceId = d.hrPortal.getId())).containsExactly(hr);
        assertThat(find(b -> b.technicianId = d.tech1.getId())).containsExactlyInAnyOrder(payment, hr);
        assertThat(find(b -> b.sla = SlaStatus.MET)).containsExactly(hr);
        assertThat(find(b -> b.from = LocalDate.of(2026, 11, 6))).hasSize(3);
        assertThat(find(b -> b.from = LocalDate.of(2026, 11, 7))).isEmpty();
        assertThat(find(b -> b.to = LocalDate.of(2026, 11, 5))).isEmpty();
    }

    @Test
    void filtersCombineWithAnd() {
        assertThat(find(b -> {
            b.categoryId = d.application.getId();
            b.status = IncidentStatus.CLOSED;
        })).containsExactly(hr);
    }

    @Test
    void resultsAreLimitedToWhatTheUserMaySee() {
        IncidentSearchCriteria all = IncidentSearchCriteria.none();
        assertThat(search.search(all, d.emp2)).containsExactly(vpn);
        assertThat(search.search(all, d.tech2)).isEmpty();          // nothing assigned to tech2
        assertThat(search.search(all, d.lead1)).hasSize(3);         // team lead sees all
    }

    @Test
    void dashboardFigures() {
        DashboardReport r = reports.overview(d.admin);
        assertThat(r.total()).isEqualTo(3);
        assertThat(r.open()).isEqualTo(2);
        assertThat(r.criticalOpen()).isEqualTo(1);
        assertThat(r.resolved()).isEqualTo(1);
        assertThat(r.slaCompliancePercent()).isEqualTo(100);
        assertThat(r.averageResolutionMinutes()).isEqualTo(30);
        assertThat(r.byPriority()).containsEntry("P1 - Critical", 1L).containsEntry("P4 - Low", 1L);
        assertThat(r.unassigned()).containsExactly(vpn);
        assertThat(r.byService()).containsKeys("Payment Gateway", "VPN", "HR Portal");
    }

    @Test
    void reports_areForAdministratorsOnly() {
        assertThatThrownBy(() -> reports.overview(d.emp1)).isInstanceOf(UnauthorizedOperationException.class);
    }
}
