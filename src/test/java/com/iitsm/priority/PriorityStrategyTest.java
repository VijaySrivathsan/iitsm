package com.iitsm.priority;

import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriorityStrategyTest {

    private final WeightedScoringPriorityStrategy weighted = new WeightedScoringPriorityStrategy();
    private final MatrixPriorityStrategy matrix = new MatrixPriorityStrategy();

    /** The worked examples from docs/plan.md section 5. */
    @ParameterizedTest(name = "{0}x{1}, criticality {2}, {3} users -> {4}")
    @CsvSource({
            "HIGH,   HIGH,   5, 500, P1",   // payment outage: 45 x 1.5 = 67.5
            "HIGH,   MEDIUM, 4,  50, P2",   // VPN: 24 x 1.2 = 28.8
            "MEDIUM, MEDIUM, 3,   1, P3",   // email: 12
            "LOW,    LOW,    2,   1, P4"    // HR portal: 2
    })
    void weighted_planExamples(Impact impact, Urgency urgency, int criticality, int users, Priority expected) {
        assertThat(weighted.calculate(new PriorityInput(impact, urgency, criticality, users)).priority())
                .isEqualTo(expected);
    }

    @Test
    void weighted_thresholdBoundaries() {
        assertThat(WeightedScoringPriorityStrategy.toPriority(30)).isEqualTo(Priority.P1);
        assertThat(WeightedScoringPriorityStrategy.toPriority(29.9)).isEqualTo(Priority.P2);
        assertThat(WeightedScoringPriorityStrategy.toPriority(15)).isEqualTo(Priority.P2);
        assertThat(WeightedScoringPriorityStrategy.toPriority(14.9)).isEqualTo(Priority.P3);
        assertThat(WeightedScoringPriorityStrategy.toPriority(6)).isEqualTo(Priority.P3);
        assertThat(WeightedScoringPriorityStrategy.toPriority(5.9)).isEqualTo(Priority.P4);
    }

    @Test
    void weighted_affectedUsersFactorBoundaries() {
        assertThat(WeightedScoringPriorityStrategy.affectedUsersFactor(9)).isEqualTo(1.0);
        assertThat(WeightedScoringPriorityStrategy.affectedUsersFactor(10)).isEqualTo(1.2);
        assertThat(WeightedScoringPriorityStrategy.affectedUsersFactor(99)).isEqualTo(1.2);
        assertThat(WeightedScoringPriorityStrategy.affectedUsersFactor(100)).isEqualTo(1.5);
        assertThat(WeightedScoringPriorityStrategy.affectedUsersFactor(999)).isEqualTo(1.5);
        assertThat(WeightedScoringPriorityStrategy.affectedUsersFactor(1000)).isEqualTo(2.0);
    }

    @Test
    void weighted_explanationShowsTheWorking() {
        PriorityResult result = weighted.calculate(new PriorityInput(Impact.HIGH, Urgency.HIGH, 5, 500));
        assertThat(result.explanation())
                .contains("Impact High(3)", "Urgency High(3)", "Criticality 5", "= 45", "x 1.5", "67.5", "P1");
    }

    @ParameterizedTest(name = "matrix {0}/{1} -> {2}")
    @CsvSource({
            "HIGH, HIGH, P1", "HIGH, MEDIUM, P2", "HIGH, LOW, P3",
            "MEDIUM, HIGH, P2", "MEDIUM, MEDIUM, P3", "MEDIUM, LOW, P4",
            "LOW, HIGH, P3", "LOW, MEDIUM, P4", "LOW, LOW, P4"
    })
    void matrix_fullTable(Impact impact, Urgency urgency, Priority expected) {
        // criticality and users are ignored by the matrix strategy
        assertThat(matrix.calculate(new PriorityInput(impact, urgency, 5, 5000)).priority()).isEqualTo(expected);
    }

    @Test
    void strategiesAreInterchangeable_samInputDifferentAnswer() {
        PriorityInput criticalServiceMediumIssue = new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 5, 2000);
        // weighted: 2x2x5 = 20 x 2.0 = 40 -> P1; matrix ignores criticality/users -> P3
        assertThat(weighted.calculate(criticalServiceMediumIssue).priority()).isEqualTo(Priority.P1);
        assertThat(matrix.calculate(criticalServiceMediumIssue).priority()).isEqualTo(Priority.P3);
    }

    @Test
    void priorityService_usesConfiguredStrategy() {
        PriorityService service = new PriorityService(List.of(weighted, matrix), "matrix");
        assertThat(service.activeStrategy()).isSameAs(matrix);
        assertThat(service.availableStrategies()).containsOnlyKeys("weighted", "matrix");
    }

    @Test
    void priorityService_rejectsUnknownStrategyName() {
        assertThatThrownBy(() -> new PriorityService(List.of(weighted, matrix), "magic"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("magic");
    }
}
