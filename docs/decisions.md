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

<!-- Add new entries below (D4, D5, ...) -->
