Milestone 5 — technician assignment (Strategy pattern + intelligent feature D). Read `CLAUDE.md` and `docs/plan.md` first.

Implement:
- `AssignmentStrategy` interface: `Optional<Technician> select(Incident, List<Technician> candidates)`.
- `SkillBasedAssignmentStrategy` (matches category, ties → least loaded), `LoadBasedAssignmentStrategy` (least active incidents, uses a `PriorityQueue` or comparator), `RoundRobinAssignmentStrategy`, and `SmartRecommendationStrategy` (weighted score: skill match, workload, experience, priority, prior incidents on the same service — feature D; returns ranked recommendations with reasons).
- `AssignmentStrategyRegistry` holding all strategies by name (Spring injects the list) and the currently active one, which an admin can change at runtime (persisted setting).
- `AssignmentService`: auto-assign on classification; manual assign/reassign by admin; respects availability and max workload → `TechnicianUnavailableException`, `WorkloadExceededException`.
- Technician workload query (active incidents per technician).

Tests: each strategy's selection incl. ties and no-candidate cases, workload limit boundary, switching active strategy changes the result.

Then follow the `milestone-done` skill.
