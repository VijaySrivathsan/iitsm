Milestone 2 — domain model and persistence. Read `CLAUDE.md` and `docs/plan.md` first.

Implement the domain model from the plan as JPA entities:
- Abstract `User` (single-table or joined inheritance — justify in `docs/decisions.md`) with `Employee`, `Technician` (skills = set of categories, max workload, availability, experience level, team-lead flag), `Administrator`.
- `ITService` (name, business criticality 1–5), `Category` (data, not enum — admin can add), `SlaPolicy`, `Incident` (all fields from the brief: reporter, category, description, service, impact, urgency, priority, affected users, created time, status, assigned technician, SLA record, investigation notes, resolution, closure info), `InvestigationNote`, `Resolution`, `Feedback`, `AuditEntry`, `Notification`.
- Enums: `Impact`, `Urgency`, `Priority` (P1–P4 with label), `IncidentStatus`, `Role`.
- Human-readable incident number (`INC1001`, ...) separate from the DB id.
- Encapsulation: no public setters on `Incident`; intent methods only (state changes come in M3). Return unmodifiable collections.
- Spring Data repositories for each aggregate root.
- `UserFactory` (Factory pattern) creating the right `User` subtype with a BCrypt-encoded password.

Tests: `@DataJpaTest` for repositories (save/load incident with notes; find technicians by skill), unit tests for `UserFactory`.

Then follow the `milestone-done` skill.
