---
name: milestone-done
description: Checklist to finish a milestone - tests pass, docs updated, class summaries, commit. Use when a milestone's work is complete or the user says "wrap up the milestone".
---

# Finish a milestone

1. Run `mvn test`. If anything fails, fix it before continuing. Report the test count.
2. If the milestone touched the web layer, start the app briefly (`mvn spring-boot:run`) and confirm it boots without errors, then stop it.
3. Run the `oop-check` skill on the changed code.
4. Update docs:
   - tick items in `docs/requirements-checklist.md`
   - add rows to `docs/test-cases.md` for new meaningful tests
   - add a `docs/decisions.md` entry for any notable design choice
   - mark the milestone done in `docs/plan.md`
5. Print a **2-line summary for every new class**: line 1 = what it is/does, line 2 = which OOP principle or pattern it demonstrates.
6. Commit: `git add -A && git commit -m "feat(mN): <short description>"`.
