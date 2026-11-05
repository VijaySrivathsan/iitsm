package com.iitsm.domain.incident;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lifecycle behaviour of the Incident object itself (fields change only on legal transitions).
 */
class IncidentLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final SlaPolicy POLICY = SlaPolicy.defaultFor(Priority.P1, 5, 30);

    private final Employee reporter = new Employee("emp1", "hash", "Asha Rao", null, null, "Finance");
    private final Technician tech = new Technician("tech1", "hash", "Karan Shah", null, null, 5, 4);
    private final Category application = new Category("Application", null, null);

    private Incident newIncident() {
        return new Incident(new IncidentNumber(1001), "Payment app down", "Users cannot pay", reporter,
                new ITService("Payment Gateway", null, 5), Impact.HIGH, Urgency.HIGH, 500, NOW);
    }

    @Test
    void happyPath_reportToClose() {
        Incident incident = newIncident();
        incident.classify(application, Priority.P1, "score 67.5", POLICY);
        incident.assignTo(tech);
        incident.acknowledge(NOW);
        incident.startWork();
        incident.requestInfo(tech, "Which browser?", NOW);
        incident.provideInfo(reporter, "Chrome", NOW);
        incident.resolve(new Resolution("Restarted payment service", "Memory leak", tech, NOW));
        incident.verify(new Feedback(5, "Quick fix", NOW));
        incident.close("Done", NOW);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getCategory()).isEqualTo(application);
        assertThat(incident.getPriority()).isEqualTo(Priority.P1);
        assertThat(incident.isAssignedTo(tech)).isTrue();
        assertThat(incident.getResolution().getSummary()).isEqualTo("Restarted payment service");
        assertThat(incident.getFeedback().getRating()).isEqualTo(5);
        assertThat(incident.getClosedAt()).isEqualTo(NOW);
        assertThat(incident.getNotes()).extracting(InvestigationNote::getType)
                .containsExactly(NoteType.INFO_REQUEST, NoteType.INFO_RESPONSE);
        assertThat(incident.allowedOperations()).isEmpty();
    }

    @Test
    void illegalTransition_leavesIncidentUnchanged() {
        Incident incident = newIncident();
        Resolution resolution = new Resolution("fix", null, tech, NOW);

        assertThatThrownBy(() -> incident.resolve(resolution)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> incident.assignTo(tech)).isInstanceOf(InvalidStateTransitionException.class);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(incident.getResolution()).isNull();
        assertThat(incident.getAssignee()).isNull();
    }

    @Test
    void reopen_discardsOldResolutionAndRecordsReason() {
        Incident incident = newIncident();
        incident.classify(application, Priority.P2, null, POLICY);
        incident.assignTo(tech);
        incident.acknowledge(NOW);
        incident.startWork();
        incident.resolve(new Resolution("Cleared cache", null, tech, NOW));

        incident.reopen(reporter, "Still failing", NOW);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getResolution()).isNull();
        assertThat(incident.getNotes()).last().extracting(InvestigationNote::getText)
                .isEqualTo("Reopened: Still failing");
    }

    @Test
    void allowedOperations_followCurrentState() {
        Incident incident = newIncident();
        assertThat(incident.allowedOperations()).containsExactly(IncidentOperation.CLASSIFY);
        incident.classify(application, Priority.P3, null, POLICY);
        assertThat(incident.allowedOperations())
                .containsExactlyInAnyOrder(IncidentOperation.CLASSIFY, IncidentOperation.ASSIGN);
    }
}
