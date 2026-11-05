package com.iitsm.domain.sla;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SLA rules for a P1 incident: respond within 5 minutes, resolve within 30.
 */
class SlaRecordTest {

    private static final Instant REPORTED = Instant.parse("2026-11-06T10:00:00Z");

    private SlaRecord sla;

    @BeforeEach
    void setUp() {
        sla = SlaRecord.notStarted();
        sla.start(REPORTED, Duration.ofMinutes(5), Duration.ofMinutes(30));
    }

    private static Instant at(int minutes, int seconds) {
        return REPORTED.plus(Duration.ofMinutes(minutes)).plusSeconds(seconds);
    }

    @Test
    void start_setsDeadlinesFromReportTime() {
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.ON_TRACK);
        assertThat(sla.getResponseDue()).isEqualTo(at(5, 0));
        assertThat(sla.getResolutionDue()).isEqualTo(at(30, 0));
    }

    @Test
    void responseExactlyAtDeadline_isNotABreach() {
        assertThat(sla.recordResponse(at(5, 0))).isFalse();
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.ON_TRACK);
    }

    @Test
    void responseOneSecondLate_isABreach() {
        assertThat(sla.recordResponse(at(5, 1))).isTrue();
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.BREACHED);
        assertThat(sla.getBreachedAt()).isEqualTo(at(5, 1));
    }

    @Test
    void resolutionInTime_isMet() {
        sla.recordResponse(at(2, 0));
        assertThat(sla.recordResolution(at(30, 0))).isFalse();
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.MET);
    }

    @Test
    void breachIsPermanent_evenIfResolvedInTimeLater() {
        sla.recordResponse(at(10, 0)); // late response -> breached
        sla.recordResolution(at(20, 0));
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void checkBreach_reportsOnlyOnce() {
        assertThat(sla.checkBreach(at(4, 59))).isFalse();
        assertThat(sla.checkBreach(at(6, 0))).isTrue();
        assertThat(sla.checkBreach(at(7, 0))).isFalse();
        assertThat(sla.getBreachedAt()).isEqualTo(at(5, 0)); // breached when the deadline passed
    }

    @Test
    void reopen_afterMet_returnsToOnTrackUntilDeadline() {
        sla.recordResponse(at(1, 0));
        sla.recordResolution(at(10, 0));
        assertThat(sla.reopen(at(12, 0))).isFalse();
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.ON_TRACK);
        assertThat(sla.getResolvedAt()).isNull();
    }

    @Test
    void markAtRisk_warnsOnlyOnce() {
        assertThat(sla.markAtRisk()).isTrue();
        assertThat(sla.markAtRisk()).isFalse();
        assertThat(sla.getStatus()).isEqualTo(SlaStatus.AT_RISK);
        assertThat(sla.isWarningSent()).isTrue();
    }

    @Test
    void elapsedFraction_measuresResolutionWindow() {
        assertThat(sla.elapsedFraction(REPORTED, at(15, 0))).isEqualTo(0.5);
        assertThat(sla.elapsedFraction(REPORTED, at(45, 0))).isEqualTo(1.5);
    }

    @Test
    void notStarted_ignoresEvents() {
        SlaRecord fresh = SlaRecord.notStarted();
        assertThat(fresh.recordResponse(at(100, 0))).isFalse();
        assertThat(fresh.checkBreach(at(100, 0))).isFalse();
        assertThat(fresh.getStatus()).isEqualTo(SlaStatus.NOT_STARTED);
    }
}
