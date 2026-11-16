package com.iitsm.intelligence;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentFactory;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.NewUser;
import com.iitsm.domain.user.UserFactory;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class AiIncidentAdvisorTest {

    private final AiIncidentAdvisor advisor = new AiIncidentAdvisor("", "gemini-1.5-flash");
    private final UserFactory userFactory = new UserFactory();
    private final IncidentFactory incidentFactory = new IncidentFactory(Clock.systemUTC());

    @Test
    void fallbackToLocalHeuristicWhenNoApiKey() {
        Employee reporter = userFactory.createEmployee(
                new NewUser("emp1", "demo123", "Asha Rao", "asha@iitsm.local", "+919000000001"),
                "Finance"
        );
        ITService payment = new ITService("Payment Gateway", "Card payments", 5);
        Incident incident = incidentFactory.create(
                new IncidentReport(payment, null, "Payment failure", "Transactions failing", Impact.HIGH, Urgency.HIGH, 250),
                reporter
        );

        AiIncidentAdvisor.AiAdvice advice = advisor.analyze(incident);

        assertThat(advice).isNotNull();
        assertThat(advice.isRemoteAi()).isFalse();
        assertThat(advice.summary()).contains("Payment Gateway");
        assertThat(advice.investigationSteps()).isNotEmpty();
        assertThat(advice.rootCauseHypothesis()).contains("Payment Gateway");
    }
}
