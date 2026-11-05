Milestone 4 — priority calculation and SLA. Read `CLAUDE.md` and `docs/plan.md` first.

Priority (Strategy pattern):
- `PriorityStrategy` interface → `WeightedScoringPriorityStrategy` (impact × urgency × business criticality, adjusted for affected users; thresholds from the plan) and `MatrixPriorityStrategy` (classic ITIL impact/urgency matrix). Active one chosen by config.
- `PriorityResult` value object holding the priority and a human-readable score breakdown (shown in UI later).
- Priority recalculated when impact/urgency changes; publishes a "priority changed" event.

SLA:
- `SlaPolicy` per (service, priority) with fallback default policy per priority seeded from the brief's table (P1 5m/30m, P2 15m/2h, P3 1h/8h, P4 4h/24h). Missing config → `MissingSlaConfigurationException`.
- `SlaRecord` on each incident: response deadline, resolution deadline, responded-at, resolved-at, status (ON_TRACK / AT_RISK / BREACHED / MET).
- `SlaService`: apply policy on classification; record response on acknowledge and resolution on resolve; detect breach.
- Use an injectable `java.time.Clock` everywhere so tests can control time.

Persistence: verify the `demo` profile works against PostgreSQL (`docker compose up -d`). If Docker isn't installed, note it and continue on H2.

Tests: priority boundaries for each P-level, the payment-outage example gives P1, SLA deadlines per priority, breach exactly at/after deadline (boundary), missing policy exception.

Then follow the `milestone-done` skill.
