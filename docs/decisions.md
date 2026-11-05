# Design decisions log

One entry per notable decision. Feeds the "Technology Selection Report" and viva answers.
Format: **Decision** — alternatives considered — why.

## D1. Java 21 + Spring Boot 3
Alternatives: Node/Express, Django, plain Java Swing.
Why: course is OOP in Java; Spring's DI makes Strategy/Observer/Repository natural and swappable; mature security + JPA; industry-relevant.

## D2. Server-rendered Thymeleaf UI (not React SPA)
Alternatives: React + REST.
Why: one codebase/language for a short timeline; role-specific views easy with Spring Security; REST endpoints can still be added for intelligent features. Limitation: less dynamic UI.

## D3. PostgreSQL (prod/demo) + H2 (tests)
Alternatives: MySQL, MongoDB.
Why: relational data (users, incidents, SLAs, audit) with strong integrity; JPA keeps the code DB-agnostic so tests run fast in-memory.

## D4. SINGLE_TABLE inheritance for the User hierarchy
Alternatives: JOINED, TABLE_PER_CLASS, a single User class with a role field.
Why: Employee/Technician/Administrator are genuine is-a subtypes with only a few extra columns each. One table makes login a single query with no joins and keeps the JPA mapping simple. Trade-off: subtype-only columns must be nullable.

## D5. Stateless State objects mapped from a persisted status enum
Alternatives: storing state objects directly, a big switch on status in Incident.
Why: JPA persists the simple `IncidentStatus` enum, and `IncidentStateRegistry` (an `EnumMap`) maps it to one shared, stateless `IncidentState` object. Each state overrides only the transitions it allows, so adding a state touches one class plus the registry, and illegal transitions fail in one place (`AbstractIncidentState`).

## D6. A shared `BaseEntity` superclass for identity
Alternatives: repeat `id`, `version`, `equals`/`hashCode` in every entity.
Why: every entity genuinely *is a* persistent, identifiable object, so this is an is-a relationship, not inheritance just to reuse code. One place defines id-based equality (safe for Hibernate proxies) and optimistic locking (`@Version`), so two admins editing the same incident can't silently overwrite each other. It's a `@MappedSuperclass`, so there's no extra table.

## D7. Subtype-only user fields use wrapper types (`Integer`, `Boolean`)
Because of SINGLE_TABLE (D4), a Technician's columns are NULL for Employee rows. Wrapper types accept NULL, and the getters translate NULL into safe defaults, so callers never see nulls.

## D8. Value objects for incident number, SLA record, resolution and feedback
`IncidentNumber` is a record (`INC1001`), so user-facing numbers never expose database ids. `SlaRecord`, `Resolution` and `Feedback` are `@Embeddable`: they're stored in the incident row and have no identity of their own, which is exactly composition in UML.

## D9. Who decides what in the incident lifecycle
- **State objects** decide *whether* an operation is legal in the current status and *which* status comes next. They hold no data and never touch the incident's fields.
- **`Incident`** asks its state first, then updates its own fields. If the state rejects the operation, nothing has been changed yet.
- **`IncidentService`** decides *who* may do it (reporter, assigned technician, administrator) and publishes an event afterwards.

Alternatives: one big `switch` on status inside `Incident`, or states that change the incident's fields directly.
Why: each class has a single reason to change. A new state is one new class plus one registry line. Permission rules can change without touching the lifecycle rules, and vice versa.

## D10. Domain events via Spring's `ApplicationEventPublisher` (Observer)
Alternatives: a hand-written listener list inside `IncidentService`, or calling audit and notification code directly.
Why: Spring already provides a well-tested publish/subscribe mechanism. `IncidentService` only publishes an immutable `IncidentEvent` and doesn't know who listens, so audit (M6), notifications (M6) and escalation (M8) can be added without changing it.

## D11. Priority as a Strategy, selected by configuration
Alternatives: one hard-coded formula inside `IncidentService`.
Why: the brief explicitly allows teams to choose their own algorithm. `WeightedScoringPriorityStrategy` (default) and `MatrixPriorityStrategy` (ITIL matrix) share one interface, and `PriorityService` picks the one named in `itsm.priority.strategy`. A new algorithm, such as one based on historical data, is one new `@Component`. Every result carries an explanation string, so users can see why an incident is P1.

## D12. SLA rules live in `SlaRecord`; `Incident` drives them
Alternatives: a separate service that edits SLA fields from outside.
Why: SLA timing is part of the incident's own state. `Incident.acknowledge()` records the response time and `Incident.resolve()` records the resolution time automatically, so no caller can forget. `SlaService` only chooses *which* policy applies (the service's own, else the default). The rules: meeting a deadline exactly on time is not a breach, a breach is permanent, and it is reported only once.
Known limitation: the SLA clock keeps running while the incident waits for the reporter (PENDING_INFO).

## D13. Classification happens automatically when the reporter picks a category
If the employee chooses a category when reporting, the system classifies the incident at once (priority calculated, SLA started), and the audit trail records the actor as `system`. Otherwise it stays NEW until an administrator picks a category. Either way the priority is always calculated, never typed in by hand.

## D14. Assignment strategies switchable at runtime, stored as a setting
Alternatives: a config-file choice (as for priority), or an `if/else` per strategy in the service.
Why: the brief asks that the assignment strategy can be changed without redesigning the application, and an administrator should be able to do it from the UI. `AssignmentStrategyRegistry` receives every `AssignmentStrategy` from Spring and stores the active name in the `system_setting` table, so the choice takes effect immediately and survives restarts. The four strategies are skill, load, round-robin and smart.
`AssignmentService` prepares the eligible `Candidate`s (available, below maximum workload, with workload and service history). The strategies are therefore pure functions with no database access, which makes them easy to test.
Data structures:
- `PriorityQueue` (a min-heap) for load-based assignment.
- `ConcurrentHashMap<String, AtomicInteger>` for the round-robin turn per category, which is safe if two incidents arrive at once.
If nobody suitable is free, the incident stays CLASSIFIED and a `NO_TECHNICIAN_AVAILABLE` event alerts administrators. It is never forced onto someone who is unavailable or full.

## D15. Build output kept out of iCloud (`target.nosync`)
The project sits in `~/Documents`, which is synced by iCloud Drive. iCloud created duplicate files such as `UserFactory 2.class` in `target/` and offloaded others mid-build, which broke the tests with "Operation timed out". Maven now builds into `target.nosync/`, and iCloud does not sync folders whose names end in `.nosync`. Moving the whole project outside `~/Documents` would be safer still.

## D16. Audit and notifications are Observers, run in the same transaction
`AuditListener` and `NotificationListener` are Spring `@EventListener`s on `IncidentEvent`. They run synchronously, inside the transaction that changed the incident.
- **Audit:** an incident change and its audit line commit together or not at all, so the trail can never miss a change. `@Order` makes audit run before notifications.
- **Notifications:** every channel call is wrapped in try/catch, so a failing email or SMS provider is logged and skipped. It never rolls back the incident change or blocks the other channels (test: `FailingSmsGatewayTest`).
Alternative considered: `@TransactionalEventListener(AFTER_COMMIT)` with asynchronous delivery. It's better for real email at scale, because messages are sent only after a successful commit and slow providers don't delay users. It's more complex, and the gateways here are simulated, so it's noted as a scaling step.

## D17. Notification design: Strategy (channels) + Adapter (gateways) + Factory (messages) + rule table (recipients)
- **`NotificationChannel`** (Strategy): in-app, email and SMS share one interface. Each channel decides in `accepts()` whether it applies; for example, SMS is used only for urgent messages to users with a phone number.
- **`EmailGateway` / `SmsGateway`** (Adapter targets): the channels depend only on these interfaces. The `Logging…Gateway` classes simulate a provider and keep an outbox for administrators. A real provider means one new adapter class.
- **`NotificationFactory`** (Factory): builds the message text per event in one place, and decides urgency (SLA problems, escalations, P1/P2 assignment).
- **Recipients:** an `EnumMap<IncidentEventType, Set<Audience>>` rule table in `NotificationListener`. The person who caused an event is never notified about it.
- **Channels per role:** configurable via `itsm.notifications.channels.<ROLE>`. By default employees get in-app and email; technicians and administrators also get SMS for urgent messages.
Adding a push channel = one `NotificationChannel` class + one `ChannelType` value. No existing class changes.

<!-- Add new entries below (D18, D19, ...) -->
