package com.iitsm.sla;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature E rules on a P3 incident (respond 60 min, resolve 480 min), acknowledged at once.
 */
class ThresholdSlaRiskEvaluatorTest {

    private static final Instant T0 = Instant.parse("2026-11-06T10:00:00Z");
    private final ThresholdSlaRiskEvaluator evaluator = new ThresholdSlaRiskEvaluator(0.75);
    private final Technician tech = new Technician("tech1", "h", "Karan", null, null, 5, 3);

    private Incident acknowledgedP3() {
        Incident incident = new Incident(new IncidentNumber(1001), "Slow", "Slow", new Employee("e", "h", "E", null,
                null, null), new ITService("Email", null, 3), Impact.MEDIUM, Urgency.MEDIUM, 1, T0);
        incident.classify(new Category("Software", null, null), Priority.P3, null, SlaPolicy.defaultFor(Priority.P3, 60, 480));
        incident.assignTo(tech);
        incident.acknowledge(T0.plusSeconds(60));
        return incident;
    }

    private static Instant minutes(long m) {
        return T0.plus(Duration.ofMinutes(m));
    }

    @Test
    void notAtRiskBefore75Percent_atRiskFrom75Percent() {
        Incident incident = acknowledgedP3();
        assertThat(evaluator.isAtRisk(incident, minutes(359), 0)).isFalse(); // 74.8%
        assertThat(evaluator.isAtRisk(incident, minutes(360), 0)).isTrue();  // 75%
    }

    @Test
    void busyTechnician_makesItRiskyFromHalfway() {
        Incident incident = acknowledgedP3();
        assertThat(evaluator.isAtRisk(incident, minutes(240), 3)).isFalse(); // 50%, 3/5 busy
        assertThat(evaluator.isAtRisk(incident, minutes(240), 4)).isTrue();  // 50%, 4/5 = 80% busy
        assertThat(evaluator.isAtRisk(incident, minutes(239), 5)).isFalse(); // under 50%
    }

    @Test
    void unacknowledgedIncident_isRiskyAt75PercentOfResponseWindow() {
        Incident incident = new Incident(new IncidentNumber(1002), "x", "x", new Employee("e", "h", "E", null, null,
                null), new ITService("Email", null, 3), Impact.MEDIUM, Urgency.MEDIUM, 1, T0);
        incident.classify(new Category("Software", null, null), Priority.P3, null, SlaPolicy.defaultFor(Priority.P3, 60, 480));
        assertThat(evaluator.isAtRisk(incident, minutes(44), 0)).isFalse();
        assertThat(evaluator.isAtRisk(incident, minutes(45), 0)).isTrue();
    }
}
