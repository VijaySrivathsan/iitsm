package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.iitsm.domain.incident.IncidentOperation.ACKNOWLEDGE;
import static com.iitsm.domain.incident.IncidentOperation.ASSIGN;
import static com.iitsm.domain.incident.IncidentOperation.CLASSIFY;
import static com.iitsm.domain.incident.IncidentOperation.CLOSE;
import static com.iitsm.domain.incident.IncidentOperation.PROVIDE_INFO;
import static com.iitsm.domain.incident.IncidentOperation.REOPEN;
import static com.iitsm.domain.incident.IncidentOperation.REQUEST_INFO;
import static com.iitsm.domain.incident.IncidentOperation.RESOLVE;
import static com.iitsm.domain.incident.IncidentOperation.START_WORK;
import static com.iitsm.domain.incident.IncidentOperation.VERIFY;
import static com.iitsm.domain.incident.IncidentStatus.ACKNOWLEDGED;
import static com.iitsm.domain.incident.IncidentStatus.ASSIGNED;
import static com.iitsm.domain.incident.IncidentStatus.CLASSIFIED;
import static com.iitsm.domain.incident.IncidentStatus.CLOSED;
import static com.iitsm.domain.incident.IncidentStatus.IN_PROGRESS;
import static com.iitsm.domain.incident.IncidentStatus.NEW;
import static com.iitsm.domain.incident.IncidentStatus.PENDING_INFO;
import static com.iitsm.domain.incident.IncidentStatus.RESOLVED;
import static com.iitsm.domain.incident.IncidentStatus.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the complete transition table: for EVERY (status, operation) pair, legal ones lead to the
 * expected status and all others throw InvalidStateTransitionException. 9 x 10 = 90 cases.
 */
class IncidentStateTest {

    /** The lifecycle from docs/plan.md section 4, as data. Anything not listed is illegal. */
    private static final Map<IncidentStatus, Map<IncidentOperation, IncidentStatus>> LEGAL = new EnumMap<>(IncidentStatus.class);

    static {
        allow(NEW, CLASSIFY, CLASSIFIED);
        allow(CLASSIFIED, CLASSIFY, CLASSIFIED);
        allow(CLASSIFIED, ASSIGN, ASSIGNED);
        allow(ASSIGNED, ACKNOWLEDGE, ACKNOWLEDGED);
        allow(ASSIGNED, ASSIGN, ASSIGNED);
        allow(ACKNOWLEDGED, START_WORK, IN_PROGRESS);
        allow(ACKNOWLEDGED, ASSIGN, ASSIGNED);
        allow(IN_PROGRESS, REQUEST_INFO, PENDING_INFO);
        allow(IN_PROGRESS, RESOLVE, RESOLVED);
        allow(IN_PROGRESS, ASSIGN, ASSIGNED);
        allow(PENDING_INFO, PROVIDE_INFO, IN_PROGRESS);
        allow(RESOLVED, VERIFY, VERIFIED);
        allow(RESOLVED, REOPEN, IN_PROGRESS);
        allow(VERIFIED, CLOSE, CLOSED);
    }

    private static void allow(IncidentStatus from, IncidentOperation op, IncidentStatus to) {
        LEGAL.computeIfAbsent(from, k -> new EnumMap<>(IncidentOperation.class)).put(op, to);
    }

    private static IncidentStatus apply(IncidentState state, IncidentOperation op) {
        return switch (op) {
            case CLASSIFY -> state.classify();
            case ASSIGN -> state.assign();
            case ACKNOWLEDGE -> state.acknowledge();
            case START_WORK -> state.startWork();
            case REQUEST_INFO -> state.requestInfo();
            case PROVIDE_INFO -> state.provideInfo();
            case RESOLVE -> state.resolve();
            case VERIFY -> state.verify();
            case REOPEN -> state.reopen();
            case CLOSE -> state.close();
        };
    }

    static Stream<Arguments> allPairs() {
        List<Arguments> pairs = new ArrayList<>();
        for (IncidentStatus status : IncidentStatus.values()) {
            for (IncidentOperation op : IncidentOperation.values()) {
                pairs.add(Arguments.of(status, op));
            }
        }
        return pairs.stream();
    }

    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("allPairs")
    void transitionTable(IncidentStatus status, IncidentOperation op) {
        IncidentState state = IncidentStateRegistry.forStatus(status);
        IncidentStatus expected = LEGAL.getOrDefault(status, Map.of()).get(op);

        if (expected != null) {
            assertThat(apply(state, op)).isEqualTo(expected);
            assertThat(state.allows(op)).isTrue();
        } else {
            assertThatThrownBy(() -> apply(state, op))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .satisfies(e -> {
                        InvalidStateTransitionException ex = (InvalidStateTransitionException) e;
                        assertThat(ex.getCurrentStatus()).isEqualTo(status);
                        assertThat(ex.getOperation()).isEqualTo(op);
                    });
            assertThat(state.allows(op)).isFalse();
        }
    }

    @Test
    void everyStatusHasAStateObject() {
        for (IncidentStatus status : IncidentStatus.values()) {
            assertThat(IncidentStateRegistry.forStatus(status).status()).isEqualTo(status);
        }
    }

    @Test
    void closedState_allowsNothing() {
        assertThat(IncidentStateRegistry.forStatus(CLOSED).allowedOperations()).isEmpty();
    }

    @Test
    void newToClosed_isRejectedWithReadableMessage() {
        assertThatThrownBy(() -> IncidentStateRegistry.forStatus(NEW).close())
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Cannot close an incident that is NEW");
    }
}
