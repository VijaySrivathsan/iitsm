Milestone 7 — security and role-based web UI. Read `CLAUDE.md` and `docs/plan.md` first.

Security:
- Replace the temporary config: form login, logout, self-registration for employees only, BCrypt, CSRF on, `/employee/**`, `/tech/**`, `/admin/**` restricted by role, `@PreAuthorize` on sensitive service methods. Disable H2 console outside `dev`.
- Users can only see their own incidents (employee) / assigned ones (technician).
- `GlobalExceptionHandler` (`@ControllerAdvice`) mapping `ItsmException` subtypes to friendly error pages/flash messages; 403 and 404 pages.
- Bean Validation on all forms with field errors shown.

UI (Thymeleaf, one shared layout, clean CSS — Bootstrap via WebJars or CDN is fine; different nav per role):
- Employee: dashboard, report incident, my incidents, incident detail (status timeline, add info, answer info request, verify/feedback).
- Technician: assigned queue sorted by SLA urgency, incident detail (acknowledge, start, notes, request info, resolve), my workload.
- Admin: dashboard placeholder, manage users/technicians, services, categories, SLA policies, choose assignment strategy, all incidents, audit log, notification outbox.
- Notification bell with unread count on every page.

Tests: `@WebMvcTest`/`spring-security-test` — anonymous redirected to login, employee forbidden from `/admin/**`, technician can't resolve someone else's incident, invalid form shows errors.

Manually run the app and click through every page once. Then follow the `milestone-done` skill.
