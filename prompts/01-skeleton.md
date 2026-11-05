Milestone 1 — project skeleton. Read `CLAUDE.md` and `docs/plan.md` first.

Create:
- `pom.xml`: Java 21, Spring Boot 3.5.x parent; starters web, thymeleaf, security, data-jpa, validation; `thymeleaf-extras-springsecurity6`; H2 (runtime), PostgreSQL driver (runtime); test: spring-boot-starter-test, spring-security-test. Lombok is **not** allowed (we want explicit, explainable code).
- `com.iitsm.ItsmApplication` main class.
- Empty package structure from the plan (add a `package-info.java` in each with a one-line description).
- `application.yml` (H2 in-memory default), `application-dev.yml`, `application-demo.yml` (PostgreSQL at localhost:5432, db `itsm`, user `itsm`, password `itsm`).
- `docker-compose.yml` with postgres:16 matching the demo profile.
- Temporary security config permitting `/` and `/h2-console/**`, and a minimal home page (`templates/index.html`) saying "I-ITSM is running".
- `.gitignore` for Maven/IntelliJ/VS Code/macOS.
- `README.md`: what it is, prerequisites, how to test, how to run (H2 and demo profile).
- One smoke test: context loads + GET `/` returns 200.

Done when: `mvn test` passes and `mvn spring-boot:run` serves http://localhost:8080. Then follow the `milestone-done` skill.
