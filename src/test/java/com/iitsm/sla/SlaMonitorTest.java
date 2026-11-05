package com.iitsm.sla;

import com.iitsm.domain.escalation.Escalation;
import com.iitsm.domain.escalation.EscalationLevel;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventType;
import com.iitsm.intelligence.EscalationService;
import com.iitsm.repository.NotificationRepository;
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

/**
 * Continuous SLA monitoring with a controllable clock: risk warnings (E), breaches and the escalation
 * chain (C). P1 = respond in 5 min, resolve in 30 min.
 */
@ServiceLayerTest
class SlaMonitorTest {

    @Autowired private IncidentService incidents;
    @Autowired private SlaMonitorService monitor;
    @Autowired private EscalationService escalation;
    @Autowired private NotificationRepository notifications;
    @Autowired private TestEntityManager em;
    @Autowired private ApplicationEvents events;
    @Autowired private MutableClock clock;

    private TestData d;

    @BeforeEach
    void setUp() {
        clock.set(ServiceLayerTestConfig.START);
        d = new TestData(em);
    }

    private Incident p1() {
        return incidents.report(d.emp1, d.payment.getId(), d.application.getId(),
                new IncidentReport("Payment down", "Cannot pay", Impact.HIGH, Urgency.HIGH, 500));
    }

    private long eventsOf(IncidentEventType type) {
        return events.stream(IncidentEvent.class).filter(e -> e.type() == type).count();
    }

    private void at(int minutes) {
        clock.set(ServiceLayerTestConfig.START.plus(Duration.ofMinutes(minutes)));
    }

    @Test
    void riskWarningAt75Percent_thenBreachAtDeadline_eachReportedOnce() {
        Incident incident = p1();
        at(1);
        incidents.acknowledge(incident.getNumber().toString(), d.tech1);

        at(22);                        // 73% of 30 min
        assertThat(monitor.runChecks().warnings()).isZero();
        at(23);                        // 77%
        assertThat(monitor.runChecks().warnings()).isEqualTo(1);
        assertThat(incident.getSlaRecord().getStatus()).isEqualTo(SlaStatus.AT_RISK);
        at(24);
        assertThat(monitor.runChecks().warnings()).isZero(); // warned only once

        at(31);
        assertThat(monitor.runChecks().breaches()).isEqualTo(1);
        at(32);
        assertThat(monitor.runChecks().breaches()).isZero();  // breach reported only once
        assertThat(incident.getSlaRecord().getStatus()).isEqualTo(SlaStatus.BREACHED);
        assertThat(eventsOf(IncidentEventType.SLA_APPROACHING)).isEqualTo(1);
        assertThat(eventsOf(IncidentEventType.SLA_VIOLATED)).isEqualTo(1);
    }

    @Test
    void unacknowledgedP1_escalatesTeamLeadThenAdministratorThenItManager() {
        Incident incident = p1(); // assigned to tech1, never acknowledged

        at(5);
        assertThat(monitor.runChecks().escalations()).isZero();   // exactly at the deadline: not yet
        at(6);
        assertThat(monitor.runChecks().escalations()).isEqualTo(1);
        at(8);
        assertThat(monitor.runChecks().escalations()).isZero();   // next level waits one more window
        at(11);
        assertThat(monitor.runChecks().escalations()).isEqualTo(1);
        at(16);
        assertThat(monitor.runChecks().escalations()).isEqualTo(1);
        at(30);
        assertThat(monitor.runChecks().escalations()).isZero();   // top of the chain reached

        assertThat(escalation.historyOf(incident))
                .extracting(Escalation::getLevel, e -> e.getEscalatedTo().getUsername())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(EscalationLevel.TEAM_LEAD, "lead1"),
                        org.assertj.core.groups.Tuple.tuple(EscalationLevel.ADMINISTRATOR, "sysadmin"),
                        org.assertj.core.groups.Tuple.tuple(EscalationLevel.IT_MANAGER, "admin"));
        // each escalation target was notified in-app
        assertThat(notifications.findByRecipientAndChannelOrderByCreatedAtDesc(d.lead1, ChannelType.IN_APP))
                .anyMatch(n -> n.getSubject().contains("escalated"));
    }

    @Test
    void acknowledgingStopsTheEscalationChain() {
        Incident incident = p1();
        at(6);
        monitor.runChecks();                       // level 1
        incidents.acknowledge(incident.getNumber().toString(), d.tech1);
        at(20);
        assertThat(monitor.runChecks().escalations()).isZero();
        assertThat(escalation.historyOf(incident)).hasSize(1);
    }

    @Test
    void lowPriorityIncidents_areNeverEscalated() {
        incidents.report(d.emp1, d.hrPortal.getId(), d.application.getId(),
                new IncidentReport("Typo on HR page", "Minor", Impact.LOW, Urgency.LOW, 1)); // P4
        at(60 * 24);
        assertThat(monitor.runChecks().escalations()).isZero();
    }
}
