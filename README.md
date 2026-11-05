# I-ITSM: Intelligent IT Service Management & Incident Resolution System

CS F213 / MAC F212 OOP project, Semester I 2026-27.

I-ITSM manages the full lifecycle of IT incidents:

**report → classify → prioritise → assign → investigate → resolve → verify → close**

Around that lifecycle it provides:
- SLA monitoring;
- notifications by in-app message, email and SMS (simulated);
- a complete audit trail;
- search and dashboards;
- seven intelligent features (duplicate detection, correlation, automatic escalation, smart technician recommendation, SLA risk detection, category suggestion and suggested resolutions).

| Document | |
|---|---|
| Project report (draft) | [docs/report/report.md](docs/report/report.md) |
| Diagrams (architecture, class, state, ER, sequence) | [docs/report/diagrams.md](docs/report/diagrams.md), [use-case.puml](docs/report/use-case.puml) |
| Design plan | [docs/plan.md](docs/plan.md) |
| Design decisions (D1-D20) | [docs/decisions.md](docs/decisions.md) |
| Requirements checklist | [docs/requirements-checklist.md](docs/requirements-checklist.md) |
| Documented test cases (97) | [docs/test-cases.md](docs/test-cases.md) |
| Demo accounts | [docs/demo-accounts.md](docs/demo-accounts.md) |

## Tech stack
Java 21 · Spring Boot 3.5 · Spring MVC + Thymeleaf + Bootstrap 5 · Spring Security · Spring Data JPA / Hibernate · PostgreSQL (demo) / H2 (tests, dev) · JUnit 5 · Maven

## Prerequisites
- JDK 21: `java -version`.
- Maven 3.9+: `mvn -v` should report Java 21.
- Docker Desktop: only needed for the PostgreSQL `demo` profile.

## Run the tests
```bash
mvn test
```
This runs 241 tests. The build output goes to `target.nosync/`, so iCloud doesn't sync it.

## Run the app
In-memory H2, no setup needed:
```bash
mvn spring-boot:run
```
H2 with the database console at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:itsm`):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
PostgreSQL demo:
```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```
Wait for the log line **"Demo data created"**, then open http://localhost:8080 and log in with a demo account. All passwords are `demo123`, for the local demo only.

| Role | Usernames |
|---|---|
| Employee | `emp1`, `emp2`, `emp3` |
| Technician | `tech1`, `tech2`, `tech3`, `lead1` (team lead) |
| Administrator | `sysadmin`, `admin` (IT manager) |

Demo data (accounts, 7 services, 9 categories and 28 past incidents) is only created on an empty database. For the demo, Payment Gateway P1 incidents have a short SLA (respond in 2 min, resolve in 10), so escalation can be shown within a few minutes. Turn this off with `itsm.demo-data.fast-payment-sla=false`.

## Profiles
| Profile | Database | Use |
|---|---|---|
| (default) | H2 in-memory | tests, quick runs |
| `dev` | H2 in-memory + console + SQL log | development |
| `demo` | PostgreSQL via `docker-compose.yml` | demo video |

## Features mapped to the brief
| Brief requirement | Implemented as |
|---|---|
| User management (3 roles) | Self-registration for employees; admin user management; different GUI per role |
| Incident management | Create, view, update, assign, resolve, close, plus notes, information requests, verify and reopen |
| Priority management | Weighted score (impact × urgency × criticality × users) → P1-P4 with explanation; alternative ITIL matrix strategy |
| ≥2 assignment strategies | skill, load, round-robin, smart recommendation; switchable at runtime by an admin |
| SLA management | Default and per-service policies; deadlines; response/resolution tracking; warnings; violation detection; background monitor |
| ≥2 notification mechanisms | In-app (bell), email and SMS via adapter gateways (simulated, with an outbox) |
| Audit | Every event: time, user, action, incident, previous/new status, description |
| ≥5 search criteria | 10 combinable filters, scoped by role |
| ≥2 intelligent features | A duplicates, B correlation, C escalation, D smart recommendation, E SLA risk, F category suggestion, G suggested resolutions |
| Persistence | JPA entities on PostgreSQL / H2 |
| Exceptions | `ItsmException` hierarchy + `GlobalExceptionHandler` |
| Security | BCrypt, roles, `@PreAuthorize`, ownership checks, CSRF, validation |

## Design patterns
| Pattern | Where |
|---|---|
| State | `domain.incident.state`: `IncidentState`, `AbstractIncidentState`, 9 states, `IncidentStateRegistry` |
| Strategy | `PriorityStrategy`, `AssignmentStrategy`, `NotificationChannel`, plus `DuplicateDetector` and `SlaRiskEvaluator` |
| Observer | `IncidentEvent` published by services → `AuditListener`, `NotificationListener` |
| Factory | `IncidentFactory`, `UserFactory`, `NotificationFactory` |
| Repository | `repository` package (Spring Data) + `IncidentSpecifications` |
| Adapter | `EmailGateway` / `SmsGateway` → logging gateways; `ItsmUserPrincipal` adapts `User` to Spring Security |

## Project structure
```
src/main/java/com/iitsm
├── domain/        entities, value objects, enums, State pattern, factories
├── service/       use cases (incidents, users, catalog, search, reports)
├── priority/      priority strategies
├── assignment/    assignment strategies, registry, workload
├── sla/           SLA policies lookup, risk evaluator, background monitor
├── intelligence/  duplicates, correlation, escalation, suggestions
├── event/         IncidentEvent + publisher (Observer)
├── audit/         audit listener and queries
├── notification/  channels, gateways (Adapter), factory, listener
├── repository/    Spring Data repositories + search specifications
├── security/      login support, current user
├── web/           controllers, forms, exception handler
├── bootstrap/     demo data
└── config/        security, password, clock, scheduling
```
