# I-ITSM design plan

## Progress
- [x] M0 Planning (`prompts/00-kickoff.md`)
- [x] M1 Skeleton (`prompts/01-skeleton.md`): target Nov 6
- [x] M2 Domain model (`prompts/02-domain-model.md`): target Nov 7
- [x] M3 Lifecycle / State pattern (`prompts/03-lifecycle.md`): target Nov 8–9
- [x] M4 Priority + SLA (`prompts/04-priority-sla.md`): target Nov 10
- [x] M5 Assignment strategies (`prompts/05-assignment.md`): target Nov 11
- [ ] M6 Notifications + audit (`prompts/06-notifications-audit.md`): target Nov 12
- [ ] M7 Security + UI (`prompts/07-security-ui.md`): target Nov 13–15
- [ ] M8 Search, reports, intelligence (`prompts/08-search-reports-intelligence.md`): target Nov 16–17
- [ ] M9 Demo data, tests, docs (`prompts/09-demo-data-docs.md`): target Nov 18–19
- [ ] Video + PDF report: Nov 20–21 (hard deadline Nov 22)

## Context
This is the CS F213 OOP project: an Intelligent IT Service Management & Incident Resolution System. The **hard deadline is 22 Nov 2026**, for both the PDF report and a 15–20 min video, so there are 17 days left. Marks depend heavily on OOP design: every class must be explainable in the viva. The repo currently contains only the Claude Code kit (`CLAUDE.md`, `.claude/`, `docs/`, `prompts/`).

This plan fixes the design that milestones M1–M9 (`prompts/01..09`) will build. Once it's approved, it will be written to `docs/plan.md` with a checkbox per milestone and committed as `docs: add design plan`.

Stack (already decided in `docs/decisions.md`): Java 21, Spring Boot 3.5, Thymeleaf, Spring Security, Spring Data JPA, PostgreSQL (demo) and H2 (tests and dev).

---

## 1. Package structure (`com.iitsm`)
| Package | Contents |
|---|---|
| `config` | `SecurityConfig`, `ClockConfig` (injectable `Clock`), `SchedulingConfig` |
| `domain.user` | `User` (abstract), `Employee`, `Technician`, `Administrator`, `Role`, `UserFactory` |
| `domain.catalog` | `ITService`, `Category` |
| `domain.incident` | `Incident`, `IncidentNumber`, `IncidentStatus`, `Impact`, `Urgency`, `Priority`, `InvestigationNote`, `NoteType`, `Resolution`, `Feedback`, `IncidentFactory` |
| `domain.incident.state` | `IncidentState`, `AbstractIncidentState`, 9 concrete states, `IncidentStateRegistry` |
| `domain.sla` | `SlaPolicy`, `SlaRecord`, `SlaStatus` |
| `domain.audit` / `domain.notification` / `domain.escalation` | `AuditEntry` / `Notification` / `Escalation`, `EscalationLevel` |
| `domain.settings` | `SystemSetting` (key/value, e.g. the active assignment strategy) |
| `event` | `IncidentEvent`, `IncidentEventType` |
| `repository` | One Spring Data repository per aggregate root, plus `IncidentSpecifications` for search |
| `priority` | `PriorityStrategy`, `WeightedScoringPriorityStrategy`, `MatrixPriorityStrategy`, `PriorityResult` |
| `assignment` | `AssignmentStrategy`, `SkillBasedAssignmentStrategy`, `LoadBasedAssignmentStrategy`, `RoundRobinAssignmentStrategy`, `SmartRecommendationStrategy`, `TechnicianScore`, `AssignmentStrategyRegistry`, `AssignmentService` |
| `sla` | `SlaService`, `SlaMonitor` (@Scheduled), `SlaRiskEvaluator` (interface), `ThresholdSlaRiskEvaluator` |
| `notification` | `NotificationChannel`, `InAppNotificationChannel`, `EmailNotificationChannel`, `SmsNotificationChannel`, `EmailGateway`, `SmsGateway`, `LoggingEmailGateway`, `LoggingSmsGateway`, `NotificationFactory`, `NotificationMessage`, `NotificationListener` |
| `audit` | `AuditListener`, `AuditService` |
| `intelligence` | `DuplicateDetector`, `TextSimilarityDuplicateDetector`, `TextNormalizer`, `EscalationService`; optional `CategorySuggester` and `ResolutionSuggester` |
| `service` | `IncidentService`, `UserService`, `CatalogService`, `SearchService`, `ReportService` |
| `security` | `ItsmUserDetailsService`, `CurrentUserProvider` |
| `exception` | `ItsmException` hierarchy (section 9) |
| `web` | `GlobalExceptionHandler`; controllers `AuthController`, `EmployeeController`, `TechnicianController`, `AdminController`, `IncidentController` (shared detail page); form DTOs in `web.form` |
| `bootstrap` | `DemoDataLoader` (dev and demo profiles) |

That comes to roughly 75 small classes. Each one has a single job, and packages map directly to the brief's sections, so examiners can find things easily.

## 2. Domain model
| Entity | Key fields | Relationships |
|---|---|---|
| `User` (abstract, **SINGLE_TABLE** with discriminator `role`) | username, passwordHash, fullName, email, phone, active | — |
| `Employee` extends User | department | — |
| `Technician` extends User | maxWorkload, available, experienceLevel (1–5), teamLead | **Aggregation**: `@ManyToMany Set<Category> skills` |
| `Administrator` extends User | itManager (flag for the top escalation level) | — |
| `ITService` | name, description, businessCriticality (1–5), active | — |
| `Category` | name, description, keywords (for F) | Data rows, not an enum, so admins can add categories |
| `Incident` | number (`INC1001`), title, description, impact, urgency, affectedUsers, priority, priorityExplanation, status, createdAt, closedAt, closureNote, similarityScore | **Association**: `@ManyToOne` reporter (Employee), assignee (Technician, nullable), service, category, `possibleDuplicateOf`/`duplicateOf` (self). **Composition**: `@Embedded SlaRecord`, `@Embedded Resolution`, `@Embedded Feedback`, `@OneToMany(cascade=ALL, orphanRemoval) List<InvestigationNote>` |
| `InvestigationNote` | author, type (NOTE / INFO_REQUEST / INFO_RESPONSE), text, createdAt | Owned by Incident |
| `SlaPolicy` | priority, responseMinutes, resolutionMinutes | `@ManyToOne ITService` (null means the default policy) |
| `SlaRecord` (embeddable) | responseDue, resolutionDue, respondedAt, resolvedAt, status, warningSent, breachedAt | Part of Incident |
| `AuditEntry` | timestamp, actor username, action, incidentNumber, previousStatus, newStatus, description | Stores the incident *number*, not a foreign key, so audit rows outlive any change to the incident |
| `Notification` | recipient (User), channel, subject, body, createdAt, read | — |
| `Escalation` | incident, level, escalatedTo (User), at, reason | — |
| `SystemSetting` | key, value | — |

**Why SINGLE_TABLE:** there are three subtypes with few extra columns. It means one query for login, no joins, and simple JPA. The trade-off is nullable subtype columns, which will be recorded in `decisions.md`.

`Incident` has **no public setters**. Its intent methods (`classify`, `assignTo`, `resolve`, …) delegate to the state, and its collections are exposed as unmodifiable views.

## 3. Design patterns
| Pattern | Abstraction | Implementations | Used by |
|---|---|---|---|
| **State** | `IncidentState` interface; `AbstractIncidentState` throws `InvalidStateTransitionException` for every operation by default | `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingInfoState`, `ResolvedState`, `VerifiedState`, `ClosedState` (stateless singletons in an `EnumMap` inside `IncidentStateRegistry`) | `Incident`: the persisted `status` enum maps to its state object, and each state overrides only the operations it allows |
| **Strategy** (priority) | `PriorityStrategy` | `WeightedScoringPriorityStrategy` (default), `MatrixPriorityStrategy` | `IncidentService` on classify and on impact/urgency change; the active one is set in config |
| **Strategy** (assignment) | `AssignmentStrategy` | Skill-based, load-based, round-robin, smart recommendation | `AssignmentStrategyRegistry`, where admins switch the active strategy at runtime (stored in `SystemSetting`) |
| **Strategy** (channel) | `NotificationChannel` | In-app, email, SMS | `NotificationListener` picks channels per recipient role and event |
| **Observer** | Spring `ApplicationEventPublisher` + `IncidentEvent` | `AuditListener`, `NotificationListener`, `EscalationService` (listens for acknowledgements) | `IncidentService`/`SlaMonitor` publish events and never call listeners directly |
| **Factory** | `IncidentFactory`, `UserFactory`, `NotificationFactory` | — | Report form → valid NEW incident; role → correct `User` subtype with a BCrypt password; event → message text |
| **Repository** | Spring Data interfaces | `IncidentRepository` (+ `JpaSpecificationExecutor`), etc. | Services only; controllers never touch repositories |
| **Adapter** | `EmailGateway`, `SmsGateway` | `LoggingEmailGateway`, `LoggingSmsGateway` (simulated, with an in-memory outbox); a real SMTP adapter can be dropped in | Email/SMS channels |

## 4. Incident lifecycle
| From | Operation | To | Who |
|---|---|---|---|
| NEW | classify (category + priority + SLA) | CLASSIFIED | System on create; admin may reclassify |
| CLASSIFIED | assign | ASSIGNED | System (active strategy) or admin |
| ASSIGNED / ACKNOWLEDGED / IN_PROGRESS | reassign | ASSIGNED | Admin or escalation |
| ASSIGNED | acknowledge (records SLA response) | ACKNOWLEDGED | Assigned technician |
| ACKNOWLEDGED | startWork | IN_PROGRESS | Assigned technician |
| IN_PROGRESS | requestInfo | PENDING_INFO | Assigned technician |
| PENDING_INFO | provideInfo | IN_PROGRESS | Reporter |
| IN_PROGRESS | resolve (resolution required; records SLA resolution) | RESOLVED | Assigned technician |
| RESOLVED | verify (+ optional feedback) | VERIFIED | Reporter or admin |
| RESOLVED | reopen (reason required) | IN_PROGRESS | Reporter |
| VERIFIED | close | CLOSED | Reporter or admin |

Anything else, for example NEW → CLOSED or resolving from ASSIGNED, throws `InvalidStateTransitionException`. "Who" checks are done in `IncidentService` and throw `UnauthorizedOperationException`. The states only check legality.

## 5. Priority algorithm (`WeightedScoringPriorityStrategy`)
- Impact LOW/MEDIUM/HIGH = 1/2/3, urgency LOW/MEDIUM/HIGH = 1/2/3, and business criticality 1–5 comes from the service.
- `base = impact × urgency × criticality`, which ranges from 1 to 45.
- Affected-users factor: under 10 → ×1.0; 10–99 → ×1.2; 100–999 → ×1.5; 1000 or more → ×2.0.
- `score = base × factor`. A score of 30 or more is **P1**, 15 or more is **P2**, 6 or more is **P3**, and anything lower is **P4**.
- `PriorityResult` keeps the breakdown text that the UI shows.

Worked examples:
- **Payment outage:** HIGH(3) × HIGH(3) × Payment Gateway(5) = 45, × 1.5 for ~500 users = **67.5 → P1**.
- VPN: HIGH × MEDIUM × 4 = 24, × 1.2 for 50 users = 28.8 → P2.
- Email: MEDIUM × MEDIUM × 3 = 12 → P3.
- HR portal: LOW × LOW × 2 = 2 → P4.

`MatrixPriorityStrategy` is the alternative: the classic ITIL impact × urgency matrix, which ignores criticality. It exists to show polymorphism.

## 6. Assignment strategies
Candidates are active, available technicians whose active incident count is below `maxWorkload`.
- **Skill-based:** the technician's skills include the incident category. Ties go to the lower load, then the higher experience.
- **Load-based:** fewest active incidents, using a `PriorityQueue<Technician>` ordered by load.
- **Round-robin:** rotates through matching candidates, with an `AtomicInteger` cursor per category.
- **Smart recommendation (feature D):** `score = 40·skillMatch + 25·(1 − load/max) + 15·experience/5 + 10·min(priorResolvedOnService, 5)/5 + 10·(P1/P2 and experience ≥ 4)`. It returns a ranked `List<TechnicianScore>` with reasons, which are shown to the admin on reassignment.
- Spring injects `Map<String, AssignmentStrategy>` into `AssignmentStrategyRegistry`. The admin picks one from a dropdown, and the choice is persisted in `SystemSetting`.
- If there's no candidate, the incident stays CLASSIFIED and admins are alerted. A manual assign to an unavailable or full technician throws `TechnicianUnavailableException` or `WorkloadExceededException`.

## 7. SLA
- **Lookup:** policy for (service, priority), then the default policy for (null, priority), otherwise `MissingSlaConfigurationException`. The defaults come from the brief: P1 5m/30m, P2 15m/2h, P3 1h/8h, P4 4h/24h.
- **Deadlines** are set on classify, from `createdAt`. Response is recorded on acknowledge and resolution on resolve. If a deadline is missed, the incident is marked BREACHED with `breachedAt`. A reprioritisation recalculates the deadlines.
- **`SlaMonitor`** is `@Scheduled`, every 60 s by default and configurable through `itsm.sla.monitor-interval`. For every open incident it asks `SlaRiskEvaluator` for a status:
  - ON_TRACK
  - AT_RISK, which publishes SLA_APPROACHING once
  - BREACHED, which publishes SLA_VIOLATED once
  - MET, when the incident is resolved on time
- Every time-based class uses the injectable `Clock`, so tests and the demo can control time. The demo also gets a short "demo" SLA policy.
- **Limitation, to be documented:** the SLA clock does not pause during PENDING_INFO.

## 8. Intelligent features
- **A. Duplicate detection** (`TextSimilarityDuplicateDetector`)
  - `TextNormalizer` lowercases the text, strips punctuation, removes stopwords and applies a synonym map (transaction→payment; down/unavailable/failing/unable→outage).
  - Similarity is Jaccard on token sets, +0.2 if it's the same service. Candidates are open incidents from the last 24 h.
  - At 0.5 or above, the incident is marked `possibleDuplicateOf` with its score. The technician can confirm, which sets `duplicateOf`, and the child is resolved automatically when the parent resolves. This doubles as lightweight correlation (feature B).
  - Same reporter + same service + similarity ≥ 0.9 within 1 h → `DuplicateIncidentException`.
  - Example: "payment application unavailable" vs "payment transaction failing" → {payment, application, outage} vs {payment, outage} = 0.67, +0.2 = 0.87, so it's flagged.
- **C. Automatic escalation** (`EscalationService`)
  - Applies to a P1/P2 incident still ASSIGNED (not acknowledged) after its response deadline.
  - The next level is taken from a `Deque<EscalationLevel>`: TEAM_LEAD → ADMINISTRATOR → IT_MANAGER. Each step waits one more response window.
  - Each step records an `Escalation`, notifies the target and publishes an ESCALATED event, which also lands in the audit trail. An acknowledgement stops the chain.
- **E. SLA risk detection** (`ThresholdSlaRiskEvaluator`)
  - AT_RISK when 75% or more of the SLA window has elapsed (configurable), or when 50% or more has elapsed and the assignee's load is at least 80% of their maximum.
  - A risk badge shows in the technician's queue and on the admin dashboard.
- **Optional, if time allows:**
  - **F:** `CategorySuggester` matches category keywords against the description and pre-selects a category on the report form.
  - **G:** `ResolutionSuggester` finds the top 3 similar CLOSED incidents (reusing `TextNormalizer`) and shows their resolutions to the technician.

## 9. Exception hierarchy
All of these extend `ItsmException extends RuntimeException`:

- `NotFoundException` → `IncidentNotFoundException`, `ServiceNotFoundException`, `UserNotFoundException`
- `InvalidStateTransitionException` (holds from, operation)
- `UnauthorizedOperationException`
- `InvalidInputException`
- `AssignmentException` → `TechnicianUnavailableException`, `WorkloadExceededException`
- `MissingSlaConfigurationException`
- `DuplicateIncidentException`
- `ExternalServiceException`: gateway or network failure. It's caught and logged in the channel, so the incident update still succeeds.

How errors reach the user:
- `GlobalExceptionHandler` maps each type to a flash message or error page with the correct HTTP status.
- `DataAccessException` becomes a "database unavailable" page.
- An invalid login is handled by Spring Security's failure URL (`/login?error`).
- Invalid input is caught by Bean Validation on the forms.

## 10. Security
- Roles: EMPLOYEE, TECHNICIAN, ADMIN. Form login, BCrypt, CSRF on, and session logout.
- Public: `/login`, `/register` (creates employees only) and static assets. `/employee/**` is EMPLOYEE only, `/tech/**` TECHNICIAN only and `/admin/**` ADMIN only. `/incidents/{number}` is open to any authenticated user, and the ownership check happens in the service.
- `@EnableMethodSecurity`, with `@PreAuthorize("hasRole('ADMIN')")` on admin service methods.
- `IncidentService` checks ownership: employees see only their own incidents, and technicians see only those assigned to them.
- The H2 console is enabled only in `dev`.
- Security considerations will be documented in the report: password hashing, CSRF, validation, least privilege, and what isn't done (no 2FA or rate limiting).

## 11. Milestones (target dates)
| M | Prompt | Done when | Target |
|---|---|---|---|
| 1 | `01-skeleton` | pom, profiles, docker-compose, home page, smoke test green | Nov 6 |
| 2 | `02-domain-model` | entities + repositories + `UserFactory`, `@DataJpaTest` green | Nov 7 |
| 3 | `03-lifecycle` | State pattern, exceptions, `IncidentFactory`, `IncidentService`, events; transition tests | Nov 8–9 |
| 4 | `04-priority-sla` | both priority strategies, SLA policy/record/service, Postgres profile verified | Nov 10 |
| 5 | `05-assignment` | 4 strategies + registry + workload rules, tests | Nov 11 |
| 6 | `06-notifications-audit` | audit trail, 3 channels via adapters, listener tests | Nov 12 |
| 7 | `07-security-ui` | login/register, 3 role UIs, error handling, authz tests | Nov 13–15 |
| 8 | `08-search-reports-intelligence` | 8-criteria search, dashboard, `SlaMonitor`, A + C + E | Nov 16–17 |
| 9 | `09-demo-data-docs` | seed data, ≥15 documented tests, `docs/report/`, demo walkthrough passes, tag v1.0 | Nov 18–19 |
| — | buffer | record video, assemble PDF report | Nov 20–21 |

**Risks:**
- M7 (the UI) is the biggest milestone, so it gets 3 days.
- Docker isn't installed yet. It's needed by M4; if it's missing, keep working on H2 and install Docker before M9.
- The video needs every member present, so book a recording slot by Nov 20.
- If the schedule slips, cut in this order: F/G, then the SMS channel, then UI polish.

## 12. Team split (for 3–5 members; merge rows for smaller teams)
1. **Domain model & OOP:** M2, M3, the State pattern and the class diagram.
2. **Backend services:** M4 and M5, covering priority, SLA and assignment.
3. **UI/UX & security:** M7 and the demo flow.
4. **Persistence, notifications & audit:** M6, Postgres, the ER diagram and seed data.
5. **Testing & intelligent features:** M8 and the test-case document.

Everyone runs `prompts/viva-prep.md` before the viva.

## Small kit fixes to make alongside `docs/plan.md`
- `docs/demo-accounts.md`: add a second administrator, `sysadmin`, so the escalation chain TEAM_LEAD → ADMINISTRATOR → IT_MANAGER (`admin`) has a distinct person at each level.
- `docs/decisions.md`: add D4 (SINGLE_TABLE inheritance) and D5 (stateless State objects mapped from a persisted enum).

## Verification
- Planning step: `docs/plan.md` exists, has milestone checkboxes, and is committed (`git log` shows `docs: add design plan`).
- Each later milestone uses the `milestone-done` skill: `mvn test` green, the app boots, `oop-check` passes, docs updated, commit.
- End to end (M9): with `docker compose up -d` and the `demo` profile running, walk through all 3 workflows in the `demo-script` skill.
