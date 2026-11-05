package com.iitsm.domain;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Feedback;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.NoteType;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for domain invariants (no Spring, no database).
 */
class DomainModelTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private final Employee reporter = new Employee("emp1", "hash", "Asha Rao", null, null, "Finance");
    private final ITService payment = new ITService("Payment Gateway", "Card payments", 5);

    private Incident newIncident() {
        return new Incident(new IncidentNumber(1001), "Payment app down", "Users cannot complete transactions",
                reporter, payment, Impact.HIGH, Urgency.HIGH, 500, NOW);
    }

    @Test
    void newIncident_startsInNewStatusWithSlaNotStarted() {
        Incident incident = newIncident();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(incident.getNumber()).hasToString("INC1001");
        assertThat(incident.getPriority()).isNull();
        assertThat(incident.getSlaRecord().getStatus()).isEqualTo(SlaStatus.NOT_STARTED);
        assertThat(incident.isReportedBy(reporter)).isTrue();
    }

    @Test
    void incident_rejectsMissingRequiredFields() {
        assertThatThrownBy(() -> new Incident(new IncidentNumber(1001), " ", "desc", reporter, payment,
                Impact.LOW, Urgency.LOW, 1, NOW)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Incident(new IncidentNumber(1001), "t", "desc", reporter, null,
                Impact.LOW, Urgency.LOW, 1, NOW)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Incident(new IncidentNumber(1001), "t", "desc", reporter, payment,
                Impact.LOW, Urgency.LOW, 0, NOW)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void incidentNotes_areReadOnlyFromOutside() {
        Incident incident = newIncident();
        incident.addNote(reporter, NoteType.INFO_RESPONSE, "It fails at checkout", NOW);
        assertThat(incident.getNotes()).hasSize(1);
        assertThatThrownBy(() -> incident.getNotes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void incidentNumber_parsesFormatsAndIncrements() {
        assertThat(IncidentNumber.parse("inc1024").sequence()).isEqualTo(1024);
        assertThat(IncidentNumber.next(null)).hasToString("INC1001");
        assertThat(IncidentNumber.next(1050L)).hasToString("INC1051");
        assertThatThrownBy(() -> IncidentNumber.parse("TICKET-7")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void service_criticalityMustBeWithinOneToFive() {
        assertThat(new ITService("VPN", null, 1).getBusinessCriticality()).isEqualTo(1);
        assertThat(new ITService("VPN", null, 5).getBusinessCriticality()).isEqualTo(5);
        assertThatThrownBy(() -> new ITService("VPN", null, 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new ITService("VPN", null, 6)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void technician_capacityBoundary() {
        Technician tech = new Technician("tech1", "hash", "Karan Shah", null, null, 3, 4);
        assertThat(tech.hasCapacityFor(2)).isTrue();
        assertThat(tech.hasCapacityFor(3)).isFalse();
    }

    @Test
    void technician_skillsAreReadOnlyAndCheckable() {
        Technician tech = new Technician("tech1", "hash", "Karan Shah", null, null, 3, 4);
        Category database = new Category("Database", null, "sql, query");
        tech.addSkill(database);
        assertThat(tech.hasSkill(database)).isTrue();
        assertThatThrownBy(() -> tech.getSkills().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void category_nameIsUpperCasedAndKeywordsSplit() {
        Category category = new Category("network", null, "VPN, Wifi , ,router");
        assertThat(category.getName()).isEqualTo("NETWORK");
        assertThat(category.keywordList()).containsExactly("vpn", "wifi", "router");
    }

    @Test
    void slaPolicy_rejectsInvalidTargets() {
        SlaPolicy p1 = SlaPolicy.defaultFor(Priority.P1, 5, 30);
        assertThat(p1.isDefault()).isTrue();
        assertThat(p1.resolutionTarget()).isEqualTo(Duration.ofMinutes(30));
        assertThatThrownBy(() -> SlaPolicy.defaultFor(Priority.P1, 0, 30)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> SlaPolicy.defaultFor(Priority.P1, 60, 30)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void feedback_ratingBoundaries() {
        assertThat(new Feedback(1, null, NOW).getRating()).isEqualTo(1);
        assertThat(new Feedback(5, "great", NOW).getRating()).isEqualTo(5);
        assertThatThrownBy(() -> new Feedback(0, null, NOW)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Feedback(6, null, NOW)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void status_workloadAndOpenRules() {
        assertThat(IncidentStatus.IN_PROGRESS.countsTowardsWorkload()).isTrue();
        assertThat(IncidentStatus.CLASSIFIED.countsTowardsWorkload()).isFalse();
        assertThat(IncidentStatus.RESOLVED.isOpen()).isFalse();
        assertThat(IncidentStatus.NEW.isOpen()).isTrue();
    }
}
