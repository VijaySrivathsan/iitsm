# Requirements checklist (from the brief)

Tick these off as milestones land. Milestone in brackets.

## MVP (mandatory — 60 marks rides on these)
- [ ] Users: Employee, Technician, Administrator; register/login [M2, M7]
- [ ] Incidents: create, view, update, assign, resolve, close [M3, M7]
- [x] Priority from impact × urgency × business criticality (+ affected users) → P1–P4 [M4]
- [ ] ≥2 technician assignment strategies, switchable by admin at runtime [M5]
- [ ] SLA: per service/priority policy, deadlines, response + resolution tracking, warnings, violation detection & recording [M4, M8]
- [ ] ≥2 notification mechanisms (simulated OK): in-app + email (+ SMS stretch) [M6]
- [ ] Audit: complete incident history (timestamp, user, action, incident, prev/new state, description) [M6]
- [ ] Search/filter with ≥5 criteria: ID, date, status, priority, category, service, technician, SLA status [M8]
- [ ] ≥2 intelligent features [M8]
- [ ] Persistence: users, services, incidents, technicians, SLA policies, audit, resolutions [M2, M4]

## Lifecycle
NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED
(+ PENDING_INFO when technician requests info; RESOLVED → IN_PROGRESS on reopen). Illegal jumps throw `InvalidStateTransitionException`. [M3] ✅ done (service layer; UI in M7)

## Role features
- Employee: report, list own, track, add info, respond to info requests, view resolution, give feedback
- Technician: view assigned, acknowledge, investigate, add notes, update status, request info, resolve, view workload
- Admin: manage users/technicians/services/SLA policies/categories/assignment strategy; monitor incidents, reports, audit logs, SLA violations

## Intelligent features (pick ≥2; planned)
- [ ] A. Duplicate detection (text similarity + same service + time window) [M8]
- [ ] C. Automatic escalation (unacknowledged P1/P2 → team lead → admin) [M8]
- [ ] E. SLA risk detection (warn at 75% of elapsed SLA) [M8]
- [ ] D. Smart technician recommendation (scoring strategy) [M5]
- [ ] F. Category suggestion from description (keywords) — optional
- [ ] G. Suggested resolution from similar past incidents — optional

## Exceptions to handle
Invalid login, unauthorized op, invalid incident, invalid state transition, technician unavailable, workload exceeded, service not found, missing SLA config, duplicate incident, DB failure, invalid input.

## Security
Authentication, role-based authorization, BCrypt passwords, input validation (Bean Validation), CSRF on, method-level `@PreAuthorize`.

## Deliverables (report)
1 Problem analysis · 2 Requirements spec · 3 Architecture diagram · 4 UML use case + class diagram · 5 Tech stack + justification (9 questions) · 6 Repo link · 7 ER diagram + schema + sample data · 8 ≥15 test cases · 9 Video showing 3 workflows (each member introduces self + explains a module)

## Rubric (120)
Design 10 · Functionality 60 · UX/value-add 10 · Technical depth 15 · Testing 5 · Innovation 10 · Docs & presentation 10
