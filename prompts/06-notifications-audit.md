Milestone 6 — notifications and audit (Observer + Adapter + Factory). Read `CLAUDE.md` and `docs/plan.md` first.

Audit:
- `AuditListener` (`@EventListener` on `IncidentEvent`) writing an `AuditEntry` for every event: timestamp, acting user, action, incident number, previous state, new state, description.
- `AuditService` query: full history for an incident (chronological), and filtered system-wide log for admins.

Notifications:
- `NotificationChannel` interface (Strategy) → `InAppNotificationChannel` (stores `Notification` rows, unread flag), `EmailNotificationChannel`, `SmsNotificationChannel`.
- Email and SMS go through Adapter: `EmailGateway`/`SmsGateway` interfaces with `LoggingEmailGateway`/`LoggingSmsGateway` simulated implementations (log + keep an in-memory outbox viewable by admin). Real SMTP is optional via Spring Mail behind the same interface.
- `NotificationFactory` building message content per event type.
- `NotificationListener` deciding recipients and channels per event (created → reporter; assigned → technician; priority changed; SLA approaching/violated → technician + lead; resolved → reporter; closed → reporter). Channel preferences per user role configurable.
- Adding a new channel must need only one new class (prove it in `docs/decisions.md`).

Tests: an event produces the right audit row, right recipients and channels; SMS gateway failure doesn't break the incident update (log + continue).

Then follow the `milestone-done` skill.
