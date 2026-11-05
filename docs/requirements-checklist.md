# Requirements checklist (from the brief)

Tick these off as milestones land. Milestone in brackets.

## MVP (mandatory — 60 marks rides on these)
- [x] Users: Employee, Technician, Administrator; register/login [M2, M7]
- [x] Incidents: create, view, update, assign, resolve, close [M3, M7]
- [x] Priority from impact × urgency × business criticality (+ affected users) → P1–P4 [M4]
- [x] ≥2 technician assignment strategies, switchable by admin at runtime [M5] (4 built; admin screen)
- [x] SLA: per service/priority policy, deadlines, response + resolution tracking, warnings, violation detection & recording [M4, M8]
- [x] ≥2 notification mechanisms (simulated OK): in-app + email + SMS [M6] (bell icon + inbox)
- [x] Audit: complete incident history (timestamp, user, action, incident, prev/new state, description) [M6] (incident timeline + admin log)
- [x] Search/filter with ≥5 criteria: ID, date, status, priority, category, service, technician, SLA status [M8] (10 criteria)
- [x] ≥2 intelligent features [M8] (A, B, C, D, E)
- [x] Persistence: users, services, incidents, technicians, SLA policies, audit, resolutions [M2, M4] (PostgreSQL profile still to verify with Docker)

## Lifecycle
NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED
(+ PENDING_INFO when technician requests info; RESOLVED → IN_PROGRESS on reopen). Illegal jumps throw `InvalidStateTransitionException`. [M3] ✅ done (service layer; UI in M7)

## Role features
- Employee: report, list own, track, add info, respond to info requests, view resolution, give feedback
- Technician: view assigned, acknowledge, investigate, add notes, update status, request info, resolve, view workload
- Admin: manage users/technicians/services/SLA policies/categories/assignment strategy; monitor incidents, reports, audit logs, SLA violations

## Intelligent features (pick ≥2; planned)
- [x] A. Duplicate detection (text similarity + same service + time window) [M8]
- [x] C. Automatic escalation (unacknowledged P1/P2 → team lead → admin → IT manager) [M8]
- [x] B. Incident correlation (related incidents per service, outage hotspot banner) [M8]
- [x] E. SLA risk detection (warn at 75% of elapsed SLA) [M8]
- [x] D. Smart technician recommendation (scoring strategy) [M5]
- [x] F. Category suggestion from description (keywords) [M9]
- [x] G. Suggested resolution from similar past incidents [M9]

## Exceptions to handle
Invalid login, unauthorized op, invalid incident, invalid state transition, technician unavailable, workload exceeded, service not found, missing SLA config, duplicate incident, DB failure, invalid input.

## Security ✅ (M7: login, roles, BCrypt, CSRF, validation, @PreAuthorize)
Authentication, role-based authorization, BCrypt passwords, input validation (Bean Validation), CSRF on, method-level `@PreAuthorize`.

## Deliverables (report) — drafts in docs/report/ (report.md, diagrams.md, use-case.puml); team must add names, repo link, video
1 Problem analysis · 2 Requirements spec · 3 Architecture diagram · 4 UML use case + class diagram · 5 Tech stack + justification (9 questions) · 6 Repo link · 7 ER diagram + schema + sample data · 8 ≥15 test cases · 9 Video showing 3 workflows (each member introduces self + explains a module)

## Rubric (120)
Design 10 · Functionality 60 · UX/value-add 10 · Technical depth 15 · Testing 5 · Innovation 10 · Docs & presentation 10
