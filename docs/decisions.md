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

<!-- Add new entries below (D6, D7, ...) -->
