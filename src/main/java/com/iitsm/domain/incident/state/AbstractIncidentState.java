package com.iitsm.domain.incident.state;

import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.exception.InvalidStateTransitionException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Default behaviour for every state: <b>reject</b> every operation. Concrete states override only
 * the operations they allow, so an illegal transition (e.g. NEW -> CLOSED) can never slip through.
 */
abstract class AbstractIncidentState implements IncidentState {

    private final IncidentStatus status;
    private final Set<IncidentOperation> allowed;

    protected AbstractIncidentState(IncidentStatus status, Set<IncidentOperation> allowed) {
        this.status = status;
        this.allowed = allowed.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(allowed));
    }

    @Override
    public final IncidentStatus status() {
        return status;
    }

    @Override
    public final Set<IncidentOperation> allowedOperations() {
        return allowed;
    }

    protected final IncidentStatus reject(IncidentOperation operation) {
        throw new InvalidStateTransitionException(status, operation);
    }

    @Override
    public IncidentStatus classify() {
        return reject(IncidentOperation.CLASSIFY);
    }

    @Override
    public IncidentStatus assign() {
        return reject(IncidentOperation.ASSIGN);
    }

    @Override
    public IncidentStatus acknowledge() {
        return reject(IncidentOperation.ACKNOWLEDGE);
    }

    @Override
    public IncidentStatus startWork() {
        return reject(IncidentOperation.START_WORK);
    }

    @Override
    public IncidentStatus requestInfo() {
        return reject(IncidentOperation.REQUEST_INFO);
    }

    @Override
    public IncidentStatus provideInfo() {
        return reject(IncidentOperation.PROVIDE_INFO);
    }

    @Override
    public IncidentStatus resolve() {
        return reject(IncidentOperation.RESOLVE);
    }

    @Override
    public IncidentStatus verify() {
        return reject(IncidentOperation.VERIFY);
    }

    @Override
    public IncidentStatus reopen() {
        return reject(IncidentOperation.REOPEN);
    }

    @Override
    public IncidentStatus close() {
        return reject(IncidentOperation.CLOSE);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
