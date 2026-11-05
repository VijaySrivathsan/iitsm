---
name: demo-script
description: Step-by-step script for the 15-20 minute demo video covering the 3 required workflows, with accounts, clicks and talking points. Use when rehearsing or recording the demo, or checking the app is demo-ready.
---

# Demo script (3 required workflows)

Setup: `docker compose up -d`, then `mvn spring-boot:run -Dspring-boot.run.profiles=demo`. Accounts in `docs/demo-accounts.md` (password `demo123`). Use two browser windows (one normal, one private) to show two roles side by side.

Before recording, verify each step below actually works in the running app; list any step that fails and fix it first.

## Intro (2 min)
Each member introduces themself and the module they own. Show the architecture diagram and class diagram briefly.

## Workflow 1 — Report → prioritize → assign → notify (4 min)
1. Log in as `emp1`. Report: service *Payment Gateway*, "Payment application unavailable, users cannot complete transactions", impact HIGH, urgency HIGH, ~500 affected users.
2. Show the created incident: auto category suggestion (if F built), computed priority **P1** with the score breakdown, SLA deadlines (response 5 min, resolution 30 min).
3. Talking point: priority = Strategy pattern; incident creation via Factory; lifecycle NEW → CLASSIFIED → ASSIGNED via State pattern.
4. Show auto-assignment to `tech1` and which strategy chose them (skill-based). As `admin`, switch strategy to load-based to show it's swappable.
5. Show notifications: in-app bell for `tech1`, simulated email/SMS in the notification log. Talking point: Observer + Adapter.

## Workflow 2 — Investigate → SLA monitored → resolve → user notified (4 min)
1. Log in as `tech1`. Acknowledge, start work, add investigation note, request more info.
2. As `emp1`, answer the info request.
3. Show SLA panel: time remaining, "at risk" warning (feature E).
4. As `tech1`, resolve with resolution details. As `emp1`, verify and give feedback; incident closes.
5. Show the full audit trail for the incident (timestamps, user, prev/new state).
6. Try an illegal transition (e.g. close a NEW incident via the admin screen) → friendly error from `InvalidStateTransitionException`.

## Workflow 3 — Multiple reports → duplicates/correlation → escalation (4 min)
1. As `emp2` and `emp3`, report "Payment transaction failing" and "Unable to make payment".
2. Show duplicate detection flagging them as related to the first incident (feature A) and linking them.
3. Show a P1 left unacknowledged: scheduler escalates technician → team lead → admin (feature C); show escalation notifications and audit entries.

## Reports & search (2 min)
As `admin`: dashboard (totals, open, critical, resolved, SLA violations), technician workload, incidents by service; filter by ≥5 criteria.

## Close (1 min)
Patterns used and where, test count (`mvn test`), what you'd do next at scale.
