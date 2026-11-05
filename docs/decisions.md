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

<!-- Add new entries below (D11, D12, ...) -->
