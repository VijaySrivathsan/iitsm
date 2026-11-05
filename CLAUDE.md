# I-ITSM — Intelligent IT Service Management & Incident Resolution System

OOP course project (CS F213, Sem I 2026-27). **Hard deadline: 22 Nov 2026** (PDF report + 15–20 min demo video).
OOP design quality is graded heavily: every class must be explainable in a viva.

## Stack (fixed — don't change without asking)
- Java 21, Spring Boot 3.5.x, Maven (`mvn`, not a wrapper)
- Spring Web + Thymeleaf (server-rendered UI, different views per role)
- Spring Security (form login, BCrypt, role-based access)
- Spring Data JPA; **H2** for tests and `dev` profile, **PostgreSQL** (docker compose) for `demo` profile
- JUnit 5 + AssertJ + Mockito; `@SpringBootTest` only where needed
- Base package: `com.iitsm`

## Architecture (layered)
`web` (controllers, DTOs/forms) → `service` (use cases) → `domain` (entities, enums, state, strategies) → `repository` (Spring Data)
Cross-cutting packages: `notification`, `audit`, `sla`, `assignment`, `priority`, `intelligence`, `security`, `exception`.
Controllers never touch repositories directly. Domain logic lives in domain/service classes, not controllers or templates.

## Design rules
- Required patterns, each in a clearly named place: **State** (incident lifecycle), **Strategy** (priority + assignment + notification channel), **Observer** (domain events → notifications + audit), **Factory** (incidents, notifications, users), **Repository**, **Adapter** (external email/SMS gateways).
- Inheritance only for true "is-a" (e.g. `User` → `Employee`/`Technician`/`Administrator`). Prefer composition otherwise.
- Fields private; no public setters on entities unless JPA/forms require them — expose intent-revealing methods (`incident.assignTo(tech)`), not raw setters.
- New categories / strategies / channels must be addable without editing unrelated classes (open-closed). Categories are data (DB table), not a hard-coded enum.
- Custom exceptions extend a common `ItsmException`; handled by one `@ControllerAdvice`.
- Every state change publishes a domain event; audit + notifications listen. Never call notification code directly from services.

## Workflow rules
- Work one milestone at a time from `prompts/`. Keep to its scope.
- After each change: `mvn test` must pass. The build hook compiles after every Java edit — fix errors it reports before continuing.
- Write tests alongside code; keep a running list in `docs/test-cases.md` (need ≥15 documented).
- Record notable design decisions in `docs/decisions.md` (one short entry each: decision, alternatives, why).
- End every milestone with: a 2-line summary per new class, then a git commit (`feat(mN): ...`).
- Keep this file short. Detailed plan lives in `docs/plan.md`.

## Commands
- Test: `mvn test`
- Run (H2): `mvn spring-boot:run`
- Run (Postgres demo): `docker compose up -d` then `mvn spring-boot:run -Dspring-boot.run.profiles=demo`
