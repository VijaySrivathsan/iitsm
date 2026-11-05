Milestone 3 — incident lifecycle (State pattern) and core incident service. Read `CLAUDE.md` and `docs/plan.md` first.

Implement:
- `IncidentState` interface with one class per state (`NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingInfoState`, `ResolvedState`, `VerifiedState`, `ClosedState`). Each state decides which operations are legal; illegal ones throw `InvalidStateTransitionException`. The persisted `IncidentStatus` maps to its state object (e.g. via a small registry/`EnumMap`).
- `Incident` methods delegate to the current state: `classify`, `assignTo`, `acknowledge`, `startWork`, `requestInfo`, `provideInfo`, `resolve`, `verify`, `reopen`, `close`.
- Exception hierarchy: `ItsmException` base + `InvalidStateTransitionException`, `IncidentNotFoundException`, `ServiceNotFoundException`, `UnauthorizedOperationException`, `InvalidInputException` (more later).
- `IncidentFactory` building a valid NEW incident from a report form/DTO.
- `IncidentService` with create/view/update/assign/resolve/close use cases, checking that the acting user may do the operation.
- A domain event type `IncidentEvent` (created, status changed, assigned, ...) published via Spring `ApplicationEventPublisher` on every change — listeners come in M6.

Tests: every legal transition, at least 4 illegal ones (e.g. NEW→CLOSED), service authorization (employee can't resolve), events published.

Then follow the `milestone-done` skill.
