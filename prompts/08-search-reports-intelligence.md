Milestone 8 — search, reports, SLA monitoring and intelligent features. Read `CLAUDE.md` and `docs/plan.md` first.

Search & reports:
- Incident search with combinable filters: incident number, date range, status, priority, category, service, technician, SLA status (use JPA `Specification`s). Available to admin (all) and scoped for other roles.
- Admin dashboard: totals (total, open, critical, resolved, SLA violations), technician workload table, incidents per service, incidents per category, SLA compliance %. Simple charts (Chart.js via CDN) optional.

Background SLA monitoring:
- `@Scheduled` `SlaMonitor` (every minute, interval configurable) that updates SLA status and publishes SLA-approaching / SLA-violated events.

Intelligent features (each behind an interface so algorithms are swappable):
- **A. Duplicate detection** — `DuplicateDetector`: on create, compare with open incidents on the same service in the last N hours using token-based similarity (Jaccard/cosine on normalised words, with a small synonym list e.g. payment/transaction). Above threshold → mark as "possible duplicate of INCxxxx", show to technician, allow link/merge. Exact duplicate by same reporter → `DuplicateIncidentException`.
- **C. Automatic escalation** — `EscalationPolicy`: unacknowledged P1/P2 past response deadline escalates technician → team lead → admin (chain as a `Deque`), records `Escalation`, notifies, audits.
- **E. SLA risk detection** — mark AT_RISK when elapsed time ≥ 75% of the SLA window (configurable) or technician workload makes breach likely; show risk badge in queues.
- Optional if time allows: **F.** keyword-based category suggestion on the report form; **G.** suggested resolutions from similar closed incidents.

Tests: each search filter, duplicate detection true/false cases, escalation chain with a fixed `Clock`, SLA risk threshold boundary.

Then follow the `milestone-done` skill.
