# I-ITSM: Intelligent IT Service Management & Incident Resolution System

**CS F213 / MAC F212 Object-Oriented Programming, Semester I 2026-27, project report**

> Draft written from the actual code. Fill in the group number, member names and the repository link, edit the wording to your own voice, and export to PDF.
> Diagrams: [diagrams.md](diagrams.md) (Mermaid) and [use-case.puml](use-case.puml) (PlantUML).

| | |
|---|---|
| Group | _<group no.>_ |
| Members | _<name - ID - module owned>_ (one row each) |
| Repository | _<GitHub/GitLab link>_ |
| Demo video | `<group no.>_OOP_S1_2025-2026_Project` (15-20 min) |

---

## 1. Problem analysis

**Problem.** When an IT service such as the payment application fails, organisations often handle reports by email, phone or spreadsheets. As a result:
- urgent problems wait behind trivial ones;
- the same outage is reported many times;
- nobody notices that a deadline is about to be missed;
- there is no reliable record of who did what.

**Stakeholders:**
- **Employees and customers**, who report problems and want them fixed and visible.
- **Technicians and support engineers**, who need a clear, prioritised queue.
- **Team leads**, who need visibility of their team's workload and escalations.
- **IT administrators and managers**, who need configuration, SLA compliance, reports and an audit trail.

**Existing challenges** (from the brief): delayed response, incorrect prioritisation, poor technician allocation, SLA violations, poor visibility of service health, duplicate reports, incomplete history, and difficulty spotting recurring problems.

**Proposed solution.** A web application that manages the whole incident lifecycle (report → classify → prioritise → assign → investigate → resolve → verify → close). It:
- calculates priority automatically, with the working shown;
- assigns technicians using an interchangeable strategy;
- tracks SLA deadlines in the background, warning before a breach and escalating unacknowledged critical incidents;
- notifies people through in-app, email and SMS channels;
- records every action in an audit trail;
- detects duplicate and related reports;
- gives administrators search, dashboards and configuration screens.

**Assumptions:**
- A single organisation, with users managed inside the application. Employees self-register; administrators create technicians and administrators.
- Email and SMS are simulated (the brief allows this). Messages go to an outbox that administrators can view.
- One time zone for display (Asia/Kolkata). All times are stored in UTC.
- The SLA clock keeps running while waiting for the reporter (PENDING_INFO). This is a stated simplification.
- The brief's SLA targets are the defaults (P1 5/30 min, P2 15 min/2 h, P3 1/8 h, P4 4/24 h). Administrators can override them per service.

---

## 2. Requirements specification

### 2.1 Functional requirements
| ID | Requirement | Where |
|---|---|---|
| FR1 | Users register (employees) and log in; three roles: Employee, Technician, Administrator | `AuthController`, `UserService`, `SecurityConfig` |
| FR2 | Employees report incidents (service, optional category, title, description, impact, urgency, affected users) | `EmployeeController`, `IncidentFactory`, `IncidentService.report` |
| FR3 | The system classifies incidents and calculates priority P1-P4 from impact, urgency, business criticality and affected users | `PriorityService`, `WeightedScoringPriorityStrategy` |
| FR4 | The system determines the SLA (service-specific or default), computes deadlines, and tracks response and resolution | `SlaService`, `SlaRecord` |
| FR5 | The system assigns a technician with a configurable strategy (skill, load, round-robin, smart); administrators can reassign | `AssignmentService`, `AssignmentStrategyRegistry` |
| FR6 | Technicians acknowledge, investigate (notes), request information, and resolve; reporters answer, verify with feedback, reopen, and close | `IncidentService`, `IncidentController` |
| FR7 | Illegal lifecycle transitions are prevented | State pattern (`domain.incident.state`) |
| FR8 | Relevant people are notified of important events by in-app, email and SMS | `NotificationListener`, channels, gateways |
| FR9 | Every action is recorded in an audit trail (time, user, action, incident, previous/new status, description) | `AuditListener`, `AuditService` |
| FR10 | Search with 10 combinable criteria (number, text, date range, status, priority, category, service, technician, SLA status) | `SearchService`, `IncidentSpecifications` |
| FR11 | Dashboard: totals, open, critical, resolved, SLA violations, compliance %, workload, breakdowns, at-risk list | `ReportService`, admin dashboard |
| FR12 | Background SLA monitoring with warnings before deadlines and violation recording | `SlaMonitor`, `SlaMonitorService` |
| FR13 | Intelligent features A-G (see section 6) | `intelligence` package |
| FR14 | Administrators manage users, technicians (skills, capacity, availability, team lead), services, categories, SLA policies and the assignment strategy | `AdminController`, `UserService`, `CatalogService` |

### 2.2 Non-functional requirements
- **Maintainability and extensibility:**
  - new categories are data, not code;
  - new priority algorithms, assignment strategies, notification channels and SLA-risk rules are each one new class (open-closed principle).
- **Security:** authentication, role-based authorisation, BCrypt password hashing, CSRF protection, input validation, ownership checks (section 8).
- **Reliability:**
  - an incident change and its audit line commit in the same transaction;
  - a failing notification provider never blocks an incident update;
  - optimistic locking (`@Version`) prevents lost updates.
- **Usability:**
  - a different home page per role;
  - only the actions the user may take right now are shown;
  - friendly error banners and 403/404 pages;
  - SLA countdowns.
- **Testability:** a clock is injected everywhere, so time-based rules are tested without waiting. There are 240+ automated tests.
- **Portability:** H2 for tests and development; PostgreSQL for the demo through Docker Compose. JPA keeps the code database-independent.

### 2.3 User roles
| Role | Can |
|---|---|
| Employee | register, log in, report, view and track own incidents, add information, answer requests, verify with feedback, reopen, close, search own incidents, receive notifications |
| Technician | view queue (most urgent SLA first) and workload, set availability, acknowledge, start work, add notes, request information, resolve, confirm/dismiss duplicates, see suggested resolutions |
| Team lead (a technician) | everything a technician can, plus see all incidents and team workload; first level of escalation |
| Administrator | manage users, technicians, services, categories and SLA policies; choose the assignment strategy; classify and assign; view all incidents, dashboard, audit log and outbox; verify or close on behalf of reporters |

### 2.4 Business rules
1. Priority is always calculated, never entered by hand: score = impact (1-3) × urgency (1-3) × criticality (1-5) × user factor (1.0/1.2/1.5/2.0). P1 ≥ 30, P2 ≥ 15, P3 ≥ 6, otherwise P4.
2. SLA deadlines are measured from the time of reporting. Meeting a deadline exactly on time is not a breach. A breach is permanent and reported once.
3. A technician never receives an incident while unavailable or at maximum workload. If nobody is eligible, administrators are alerted.
4. Only the assigned technician acknowledges, works on and resolves an incident. Only the reporter answers, reopens or gives feedback. Administrators classify and assign.
5. An unacknowledged P1/P2 escalates to the team lead, then the system administrator, then the IT manager, one response window apart.
6. An employee cannot file the same report again within one hour. Similar reports from others are flagged for a person to confirm.
7. Nobody is notified about their own action. SMS is used only for urgent events and never for employees.

---

## 3. System architecture

The application is a layered Spring Boot monolith (diagram: [diagrams.md §1](diagrams.md)):

**web → service → domain / strategies → repository → database**

The cross-cutting concerns (audit, notifications) hang off an internal event bus.

**Why this architecture:**
- It matches the brief's "Choice 1: layered architecture".
- It keeps every class easy to explain.
- It suits a team of 3-5 working for three weeks.

The event bus and the strategy interfaces mark where it could later be split into services (section 5, question 9).

---

## 4. UML diagrams

- **Use-case diagram:** [use-case.puml](use-case.puml). Actors are Employee, Technician, Team lead (a technician), Administrator, and the System scheduler.
- **Class diagrams:** [diagrams.md §2-3](diagrams.md). They show classes, attributes, methods, interfaces, inheritance, associations, composition and aggregation.
- **State diagram:** [diagrams.md §4](diagrams.md).
- **Sequence diagram for workflow 1:** [diagrams.md §6](diagrams.md).

### 4.1 How the OOP concepts appear
| Concept | Example in our code |
|---|---|
| Encapsulation | `Incident` has no public setters; state changes only through intent methods (`resolve`, `reopen`, …). `getNotes()` and `Technician.getSkills()` return read-only views. Constructors validate with `Checks`. |
| Abstraction | `IncidentState`, `PriorityStrategy`, `AssignmentStrategy`, `NotificationChannel`, `DuplicateDetector`, `SlaRiskEvaluator`: callers depend on what, not how. |
| Inheritance (true is-a only) | `User` → `Employee` / `Technician` / `Administrator`; `AbstractIncidentState` → 9 states; `ItsmException` → exception families; `BaseEntity` → all entities. |
| Polymorphism | `user.getRole()`; `incident.resolve()` behaves differently per state; the same `PriorityInput` gives P1 with the weighted strategy and P3 with the matrix (tested). |
| Interfaces | Ten contracts, including those above plus `EmailGateway` and `SmsGateway`. |
| Composition | `Incident` owns `SlaRecord`, `Resolution`, `Feedback` (embedded) and its `InvestigationNote`s (cascade + orphan removal). `InMemoryOutbox` is used by the gateways (has-a, not is-a). |
| Association / aggregation | An incident refers to its reporter, assignee, service and category. A technician's skills are shared `Category` objects (aggregation). |
| Exception handling | `ItsmException` hierarchy: not-found family, unauthorised, invalid input, invalid state transition, assignment family (technician unavailable, workload exceeded), missing SLA configuration, duplicate incident, external service. One `GlobalExceptionHandler` maps families to pages. |
| Collections | `EnumMap` (state registry, priority matrix, notification rules), `PriorityQueue` (load-based assignment), `ConcurrentHashMap<String, AtomicInteger>` (round-robin turns), `Deque` (escalation chain, bounded outbox), `LinkedHashMap` (ordered report breakdowns), `Set` (skills, word sets for Jaccard similarity). |

---

## 5. Technology stack and selection report

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 (Web MVC, Security, Data JPA, Validation, Scheduling) |
| UI | Thymeleaf server-side templates + Bootstrap 5 + Bootstrap Icons; a small amount of JavaScript for category suggestion |
| Persistence | Hibernate / JPA; PostgreSQL 16 (Docker Compose) for the demo; H2 in-memory for tests and development |
| Testing | JUnit 5, AssertJ, Spring Boot Test, MockMvc, Spring Security Test |
| Build | Maven |
| Tools | Git, IntelliJ / VS Code, Claude Code (AI-assisted development under our review) |

The brief's nine questions:
1. **Why this language?** Java is the course language and strongly object-oriented: interfaces, abstract classes, enums with behaviour, records for value objects, and sealed type checks with pattern matching. Each OOP requirement maps directly onto a language feature.
2. **Why this frontend technology?** Thymeleaf renders pages on the server with Spring Security integration (`sec:authorize`), so each role gets a different GUI with no separate frontend project. That was the right trade-off for a three-week deadline. Bootstrap gives a clean, responsive look with little CSS.
3. **Why this backend technology?** Spring Boot's dependency injection makes the patterns natural:
   - Spring injects every `AssignmentStrategy` into a registry (Strategy);
   - `ApplicationEventPublisher` is a ready-made Observer bus;
   - Spring Data generates the Repository implementations;
   - `@Scheduled` runs the SLA monitor.
   It is also mature and widely used in industry.
4. **Why this database?** The data is strongly relational (users ↔ incidents ↔ services ↔ SLA policies ↔ audit), with integrity constraints and transactions, which suits PostgreSQL well. H2 lets tests run in seconds. JPA means the same code runs on both.
5. **Why this architecture?** A layered monolith is the simplest structure that still separates concerns. Each layer depends only on the one below, and cross-cutting reactions go through events. It is easy to test, deploy and explain.
6. **Alternatives considered:**
   - React + REST: richer UI, but two codebases and more time.
   - Node/Express or Django: less natural for an OOP-in-Java course.
   - MongoDB: weaker fit for relational, transactional data.
   - Microservices: unnecessary operational complexity at this scale.
   - Decisions are logged in `docs/decisions.md` (D1-D20).
7. **Advantages and limitations of the stack:**
   - **Advantages:** strong typing; patterns supported by the framework; security out of the box; very testable (injected `Clock`, H2, MockMvc).
   - **Limitations:** server-rendered pages need a reload to update (no live push); open-session-in-view keeps a database connection during rendering; in-memory dashboard aggregation and a simulated email/SMS outbox are fine at this scale but not at very large scale.
8. **How the technology supports OOP design:** see the table in §4.1. Specifically:
   - JPA maps the inheritance hierarchy (SINGLE_TABLE) and composition (`@Embedded`, cascade);
   - Spring wires interfaces to implementations, so classes depend on abstractions (dependency inversion);
   - Bean Validation keeps input rules declarative.
9. **How the system would evolve if users increased significantly:**
   - run several stateless app instances behind a load balancer, with sessions in Redis;
   - add database indexes, and move dashboard figures to `GROUP BY` queries or a read replica;
   - send notifications asynchronously after commit through a message queue (Kafka/RabbitMQ);
   - run the SLA monitor on one node with a distributed lock;
   - replace the simulated gateways with real providers (one adapter class each);
   - eventually split notifications and reporting into separate services along the existing event and interface boundaries;
   - swap the rule-based duplicate and SLA-risk classes for ML models behind the same interfaces.

---

## 6. Intelligent features

The brief requires at least two. We built seven: A-G.

| | Feature | How it works | Class |
|---|---|---|---|
| A | Duplicate detection | Normalise text (stopwords, plurals, synonyms such as "transaction/checkout/pay → payment" and "down/failing/unable → outage"), then Jaccard word overlap, +0.2 for the same service, flagged at 0.5 or above. Always points at the original report. A person confirms or dismisses. The same employee repeating a report within 1 h is refused. | `TextSimilarityDuplicateDetector`, `DuplicateDetectionService` |
| B | Incident correlation | Other open incidents on the same service in the last 24 h are shown together. A "possible service outage" banner appears when a service has 2 or more open reports in an hour. | `DuplicateDetectionService.relatedOnSameService`, `ReportService` hotspots |
| C | Automatic escalation | An unacknowledged P1/P2 escalates to the team lead at the response deadline, then the system administrator, then the IT manager, one window apart. Levels with nobody available are skipped. Acknowledging stops the chain. | `EscalationService` |
| D | Smart technician recommendation | Score out of 100: skill 40 + spare capacity 25 + experience 15 + history with the service 10 + senior bonus 10 for P1/P2, with an explanation. | `SmartRecommendationStrategy` |
| E | SLA risk detection | At risk at 75% of the response window (if not acknowledged) or of the resolution window, or at 50% when the technician is at least 80% busy. Warned once. | `ThresholdSlaRiskEvaluator`, `SlaMonitorService` |
| F | Category suggestion | Category keywords (editable by admins) are matched against the description while the employee types. | `CategorySuggester`, `/api/suggest-category` |
| G | Suggested resolutions | Up to 3 similar resolved incidents and how they were fixed are shown to the technician. | `ResolutionSuggester` |

Priority calculation (`WeightedScoringPriorityStrategy`) is also automated, and it explains its result. Example: *Impact High(3) × Urgency High(3) × Criticality 5 = 45; × 1.5 for 500 affected users = 67.5 → P1 - Critical.*

**Innovation challenge items covered:**
- AI-assisted classification (F)
- resolution recommendation (G)
- incident similarity detection (A)
- predictive SLA violation detection (E)
- automated escalation (C)
- analytics dashboard
- event-driven internal architecture

---

## 7. Database design

- **ER diagram and schema:** [diagrams.md §5](diagrams.md). There are 11 tables. Hibernate generates the schema from the entities: `create-drop` on H2, `update` on PostgreSQL.
- **Key relationships:**
  - an incident has one reporter, one service, at most one category and at most one assignee;
  - an incident owns many notes and escalations;
  - technicians and categories are many-to-many (`technician_skills`);
  - an SLA policy refers to a service, or to none for the default;
  - an incident can point to another incident (`duplicate_of`).
- **Design choices:**
  - SINGLE_TABLE inheritance for users (decision D4);
  - SLA record, resolution and feedback embedded in the incident row as composition (D8);
  - the audit trail stores the incident number as text, so history is never lost;
  - a `version` column for optimistic locking.
- **Sample data:** 9 demo accounts, 7 services, 9 categories, default SLA policies plus a Payment Gateway P1 override (2 min / 10 min, for the demo), and 28 historical incidents with resolutions, feedback, breaches and audit lines (`HistoricalIncidentSeeder`).

---

## 8. Security considerations

| Requirement | Implementation |
|---|---|
| Authentication | Spring Security form login; invalid login goes to `/login?error`; disabled accounts cannot log in |
| Role-based authorisation | URL rules (`/employee/**`, `/tech/**`, `/admin/**`), plus `@PreAuthorize("hasRole('ADMIN')")` on administration services, plus ownership checks in `IncidentService` (own / assigned incident). Three independent layers. |
| Password protection | BCrypt (salted, slow hash); passwords are never logged (`NewUser.toString()` hides them) |
| Input validation | Bean Validation on every form (`@NotBlank`, `@Size`, `@Pattern`, `@Min`/`@Max`, `@Email`); domain constructors validate again |
| Secure API access | Every page and the JSON endpoint require login; CSRF tokens on every form; safe redirects (referer host checked) |
| Unauthorised operations | `UnauthorizedOperationException` returns a 403 page; the UI shows only permitted actions |
| Other | Thymeleaf escapes all output (XSS); JPA uses bound parameters (no SQL injection); the H2 console is only enabled in the `dev` profile |

**Not implemented (stated):** two-factor authentication, login rate limiting or lockout, HTTPS (would be handled by a reverse proxy in deployment), and password reset by email. The demo password `demo123` is for the local demo only.

---

## 9. Testing

There are **241 automated tests** (`mvn test`), all passing. **97 meaningful test cases are documented** in [`docs/test-cases.md`](../test-cases.md), covering every category the brief asks for:

| Category | Examples |
|---|---|
| Normal operations | full lifecycle report → close; auto-classification; auto-assignment; search filters; dashboard figures |
| Boundary conditions | SLA met exactly at the deadline vs 1 s late; priority thresholds 30 / 29.9 / 15 / 14.9 / 6 / 5.9; affected-user factor boundaries; workload at max; risk at 74.8% vs 75%; rating 0/1/5/6 |
| Invalid inputs | blank fields, invalid usernames, short passwords, out-of-range criticality and experience, invalid SLA targets, invalid forms re-shown with errors |
| Exceptions | invalid state transitions (all 76 illegal pairs), missing SLA configuration, technician unavailable, workload exceeded, duplicate report, SMS provider failure, unknown incident |
| Authorisation | employee → admin pages (403); technician acting on someone else's incident; employee viewing another's incident; CSRF missing; role-scoped search and actions |
| Business rules | breach is permanent and reported once; escalation chain and stop on acknowledge; duplicate points at the original; actor not notified of own action; SMS only for urgent events |

**Techniques used:**
- pure unit tests for the domain and strategies;
- `@DataJpaTest` with a real H2 database for repositories and services;
- a `MutableClock` to move time forward;
- `@SpringBootTest` + MockMvc + Spring Security Test for the web layer;
- recorded application events to verify the Observer flow.

**Manual testing:**
- every page was clicked through as each role;
- the three demo workflows were rehearsed;
- the live background monitor was observed: warning → breach → escalation in about 2 minutes on Payment Gateway.

Two bugs found this way were fixed with regression tests:
- escalation stalled when the only team lead was the assignee;
- start-up ordering of default SLA policies.

---

## 10. Demonstration (video)

The script is in `.claude/skills/demo-script/SKILL.md`. In short:

1. **Workflow 1:** emp1 reports the payment outage. It's prioritised P1 (working shown) and auto-assigned to tech1 (skill strategy). tech1 and emp1 are notified (in-app, email, SMS outbox). The admin switches the assignment strategy.
2. **Workflow 2:** tech1 acknowledges, investigates, and asks emp1 a question; emp1 answers. The SLA countdown and risk badge are shown. tech1 resolves (suggested past resolutions shown); emp1 verifies with feedback and closes. Then the audit trail, and an illegal transition rejected.
3. **Workflow 3:** emp2 and emp3 report the same problem in other words. Both are flagged as duplicates of INC1001, and the dashboard shows an outage hotspot. A P1 left unacknowledged escalates to the team lead after about 2 minutes, then to the system administrator.
4. Search with several filters, the dashboard, and the configuration screens.

Each member introduces themself and explains their module.

---

## 11. Limitations and future work
- **SLA clock:** it does not pause while waiting for the reporter; there are no business-hours calendars.
- **Text similarity:** it ignores word order and meaning. Embeddings or ML could replace it behind the same `DuplicateDetector` interface.
- **Simulated email/SMS:** real SMTP or SMS adapters are one class each.
- **No real-time page updates:** WebSocket or SSE push would be a next step.
- **Dashboard aggregation:** it runs in memory and would become SQL `GROUP BY` queries at large scale.
- **PostgreSQL profile:** it needs Docker to run; tests use H2.

## 12. Team contributions
| Member | Module(s) | Key classes |
|---|---|---|
| _<name>_ | Domain model & OOP, State pattern | `Incident`, `User` hierarchy, `domain.incident.state` |
| _<name>_ | Priority, SLA, assignment | `priority`, `sla`, `assignment` |
| _<name>_ | UI/UX & security | `web`, templates, `SecurityConfig` |
| _<name>_ | Persistence, notifications, audit | `repository`, `notification`, `audit`, ER design |
| _<name>_ | Testing & intelligent features | `intelligence`, test suite, `docs/test-cases.md` |
