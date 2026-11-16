package com.iitsm.intelligence;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.user.Employee;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AiIncidentAdvisorTest {

    private final AiIncidentAdvisor advisor = new AiIncidentAdvisor("", "gemini-1.5-flash");
    private final Employee reporter = new Employee("emp1", "hash", "Asha Rao", null, null, "Finance");
    private final ITService payment = new ITService("Payment Gateway", "Card payments", 5);

    private Incident newIncident() {
        return new Incident(new IncidentNumber(1001), "Payment failure", "Transactions failing",
                reporter, payment, Impact.HIGH, Urgency.HIGH, 250, Instant.parse("2026-11-06T10:00:00Z"));
    }

    @Test
    void fallbackToLocalHeuristicWhenNoApiKey() {
        Incident incident = newIncident();
        incident.classify(new Category("Application", "Apps", "app"), Priority.P1, "test",
                SlaPolicy.defaultFor(Priority.P1, 15, 60));

        AiIncidentAdvisor.AiAdvice advice = advisor.analyze(incident);

        assertThat(advice).isNotNull();
        assertThat(advice.isRemoteAi()).isFalse();
        assertThat(advice.summary()).contains("Payment Gateway").contains("P1");
        assertThat(advice.investigationSteps()).isNotEmpty();
        assertThat(advice.rootCauseHypothesis()).contains("Payment Gateway");
    }

    @Test
    void unclassifiedIncidentWithoutPriority_stillGetsAdvice() {
        Incident incident = newIncident(); // NEW: no priority until an admin classifies it

        AiIncidentAdvisor.AiAdvice advice = advisor.analyze(incident);

        assertThat(advice.summary()).contains("not yet prioritised");
        assertThat(advice.investigationSteps()).isNotEmpty();
    }
}
