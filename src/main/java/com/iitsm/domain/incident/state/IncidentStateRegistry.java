package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentStatus;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps each persisted {@link IncidentStatus} to its {@link IncidentState} object. States hold no
 * per-incident data, so one shared instance of each is enough (see docs/decisions.md D5).
 * An {@link EnumMap} is used because the keys are a small, fixed enum.
 */
public final class IncidentStateRegistry {

    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        register(new NewState());
        register(new ClassifiedState());
        register(new AssignedState());
        register(new AcknowledgedState());
        register(new InProgressState());
        register(new PendingInfoState());
        register(new ResolvedState());
        register(new VerifiedState());
        register(new ClosedState());
        if (STATES.size() != IncidentStatus.values().length) {
            throw new IllegalStateException("Every IncidentStatus needs a state class");
        }
    }

    private IncidentStateRegistry() {
    }

    private static void register(IncidentState state) {
        STATES.put(state.status(), state);
    }

    public static IncidentState forStatus(IncidentStatus status) {
        return STATES.get(status);
    }
}
