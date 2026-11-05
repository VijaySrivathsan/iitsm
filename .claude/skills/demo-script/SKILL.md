---
name: demo-script
description: Step-by-step script for the 15-20 minute demo video covering the 3 required workflows, with accounts, clicks and talking points. Use when rehearsing or recording the demo, or checking the app is demo-ready.
---

# Demo script (3 required workflows)

**Setup:**
- Start a fresh database:
  - H2: `mvn spring-boot:run`
  - or PostgreSQL: `docker compose up -d`, then `mvn spring-boot:run -Dspring-boot.run.profiles=demo`
- **Wait for the log line "Demo data created"** before logging in.
- Accounts are in `docs/demo-accounts.md`; every password is `demo123`.
- Use separate browser profiles or private windows to show two roles side by side.
- Timings in this setup:
  - **Payment Gateway P1:** respond within 2 min, resolve within 10 min (a demo override of the 5/30 default).
  - **SLA monitor:** runs every 20 s.
  - So a P1 left alone is at risk after about 1.5 min, and breached and escalated to the team lead after about 2-2.5 min.
- Before recording, walk through each step below and fix anything that fails.

## Intro (2 min)
- Each member introduces themself and the module they own.
- Show the architecture and class diagrams (`docs/report/diagrams.md`).

## Workflow 1: report → prioritise → assign → notify (4 min)
1. Log in as `emp1` and open **Report incident**.
2. Fill in:
   - service *Payment Gateway*;
   - title "Payment application unavailable";
   - description "The payment application is unavailable and users cannot complete transactions.";
   - impact High, urgency High, 500 users.
   While you type, point out the **suggested category: APPLICATION** (feature F).
3. Submit. INC1001 opens as **P1 - Critical**. Point to "How the priority was calculated" (Strategy pattern) and the SLA panel's deadlines (service-specific SLA).
4. Show the audit timeline: CREATED → CLASSIFIED → ASSIGNED to Karan Shah (`tech1`), "by 'skill' strategy".
   **Talking points:**
   - the State pattern moves the status;
   - `IncidentFactory` created the incident;
   - Observer: the timeline and notifications are listeners.
5. As `admin`:
   - **Monitor → Email/SMS outbox** shows the email and SMS to tech1 (Adapter pattern).
   - **Configure → Assignment strategy**: switch to `load` and back, to show it is swappable at runtime.

## Workflow 2: investigate → SLA monitored → resolve → user notified (4 min)
1. As `tech1`:
   - the bell shows a notification;
   - **My queue** is ordered by SLA;
   - open INC1001 and point out **Suggested resolutions from similar past incidents** (feature G).
2. Acknowledge (SLA response recorded) → Start investigation → Ask the reporter "Does it fail for card and UPI?".
3. As `emp1`, answer the question (the status goes PENDING_INFO → IN_PROGRESS).
4. As `tech1`, add a note, then Resolve ("Restarted payment pods and raised memory limit", root cause "Memory leak").
5. As `emp1`, verify with 5 stars, then close. Show the final page: CLOSED, SLA MET, resolution and feedback.
6. Show an illegal action being refused. For example, on a new incident as `admin`, the Close button isn't offered, because the State pattern only allows legal operations. Mention the test that checks all 90 transitions.

## Workflow 3: multiple reports → duplicates/correlation → escalation (4 min)
1. As `emp2`, report "Payment transaction failing" / "Customers get an error when paying at checkout" (Payment Gateway, High/High).
2. As `emp3`, report "Unable to make payment" / "Payment page shows an error".
3. Open either one: **Possible duplicate of INC1001** with a similarity score (feature A), plus **Related incidents** on the same service (feature B). As the assigned technician, click **Confirm & link**.
4. As `admin`, the Overview shows **"Possible service outage: Payment Gateway (n open reports in the last hour)"**, plus breakdowns and workload.
5. Leave one of the new P1s unacknowledged:
   - after about 1.5 min it shows **SLA AT RISK** (feature E);
   - after about 2-2.5 min it shows **SLA BREACHED** and the **Escalations** panel shows "Level 1: Team lead → Meera Pillai" (feature C);
   - log in as `lead1` to show the escalation notification;
   - one window later it escalates to `sysadmin`.

## Search & reports (2 min)
- **Search**: combine text "payment" + priority P1 + status, then filter by SLA status BREACHED.
- Admin Overview: SLA compliance %, average fix time, the at-risk list, and the history from the 28 seeded past incidents.

## Close (1 min)
- Patterns used and where (`docs/report/diagrams.md` §3).
- `mvn test`: 240+ tests passing.
- What you'd do at scale (report §5, question 9).
