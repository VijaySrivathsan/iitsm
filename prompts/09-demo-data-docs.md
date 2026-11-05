Milestone 9 — demo data, test documentation, report material, polish. Read `CLAUDE.md` and `docs/plan.md` first.

1. **Seed data** (`DemoDataLoader`, active in `dev` and `demo` profiles, idempotent): accounts exactly as in `docs/demo-accounts.md`, the listed services with criticality, categories, SLA policies, and ~30 historical incidents spread across services/statuses/priorities (some breached, some closed with resolutions) so dashboards look realistic.
2. **Demo readiness**: walk through the `demo-script` skill against the running app (`demo` profile). Fix anything that doesn't work. Make sure escalation and SLA risk can be shown within a few minutes (e.g. a demo-only short SLA policy or configurable scheduler interval).
3. **Tests**: ensure ≥15 meaningful tests documented in `docs/test-cases.md` covering normal, boundary, invalid input, exceptions, authorization, business rules. Use the `test-writer` subagent for gaps. `mvn test` green.
4. **Report material**: use the `doc-writer` subagent to generate `docs/report/` (class, use-case, state diagrams; ER diagram; architecture; problem analysis; requirements; technology selection report answering the 9 questions; security; testing summary).
5. **Review**: run the `oop-reviewer` subagent on the whole codebase; fix the top issues.
6. **README**: final run instructions, demo accounts link, feature list mapped to the brief, patterns-and-where table.

Then follow the `milestone-done` skill and tag the commit `v1.0`.
