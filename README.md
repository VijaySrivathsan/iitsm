# I-ITSM: Intelligent IT Service Management & Incident Resolution System

CS F213 / MAC F212 OOP project, Semester I 2026-27.

I-ITSM manages the full lifecycle of IT incidents: report, classify, prioritise, assign, investigate, resolve, verify and close. It adds SLA monitoring, notifications, an audit trail, reporting and intelligent features (duplicate detection, automatic escalation, SLA risk detection).

- Design plan: [docs/plan.md](docs/plan.md)
- Design decisions: [docs/decisions.md](docs/decisions.md)
- Requirements checklist: [docs/requirements-checklist.md](docs/requirements-checklist.md)

## Tech stack
Java 21 · Spring Boot 3.5 · Spring MVC + Thymeleaf · Spring Security · Spring Data JPA · PostgreSQL (demo) / H2 (tests, dev) · JUnit 5

## Prerequisites
- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -v` should report Java 21)
- Docker Desktop, only for the PostgreSQL `demo` profile

## Run the tests
```bash
mvn test
```

## Run the app
In-memory H2 (no setup needed):
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
Then open http://localhost:8080.

## Profiles
| Profile | Database | Use |
|---|---|---|
| (default) | H2 in-memory | tests, quick runs |
| `dev` | H2 in-memory + console + SQL log | development |
| `demo` | PostgreSQL via `docker-compose.yml` | demo video |
