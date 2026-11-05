We are starting the I-ITSM project. Use plan mode — do not write code yet.

Read `CLAUDE.md`, `docs/requirements-checklist.md`, `docs/decisions.md` and `docs/demo-accounts.md`. The full brief is summarised in the checklist; the deadline is 22 Nov 2026.

Produce a design plan that I will review before approving. Include:

1. **Package structure** under `com.iitsm` following the layering in CLAUDE.md.
2. **Domain model**: every entity and enum with key fields and relationships (one-to-many, many-to-many, composition vs aggregation). Include `User` hierarchy, `Incident`, `ITService`, `Category`, `SlaPolicy`, `SlaRecord`, `InvestigationNote`, `Resolution`, `Feedback`, `AuditEntry`, `Notification`, `Escalation`.
3. **Design patterns**: for each of State, Strategy (priority, assignment, notification channel), Observer, Factory, Repository, Adapter — the interface/abstract class name, its implementations, and who uses it.
4. **Incident lifecycle**: states, allowed transitions, who may trigger each, as a table.
5. **Priority algorithm**: exact formula and thresholds for P1–P4 (weighted impact × urgency × business criticality, adjusted by affected users); a worked example for the payment outage.
6. **Assignment strategies**: at least skill-based, load-based, and a scoring "smart recommendation" one; how admin switches them at runtime.
7. **SLA**: policy lookup (per service + priority with a default), deadlines, warning threshold, violation recording, scheduler.
8. **Intelligent features**: A (duplicates), C (escalation), E (SLA risk) — algorithm sketch each. F and G marked optional.
9. **Exception hierarchy**.
10. **Security**: roles, URL rules, method-level checks.
11. **Milestones M1–M9** mapped to `prompts/01..09`, with what "done" means for each. Flag any risk to the deadline.
12. **Team split** suggestion for 3–5 members.

Keep class count sensible — a student must be able to explain every class. After I approve, write the plan to `docs/plan.md` (with a checkbox per milestone), and commit it with `docs: add design plan`.
