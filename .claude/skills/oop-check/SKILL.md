---
name: oop-check
description: Quick OOP and design-pattern self-check of recently changed Java code before committing a milestone. Use at the end of each milestone or when the user asks whether the design is sound.
---

# OOP check

1. Run `git diff --stat HEAD` (or `git status` if nothing committed yet) to find changed Java files.
2. For each new/changed class, confirm:
   - Fields private; behaviour exposed through intent-revealing methods; collections returned unmodifiable.
   - It belongs in the right layer (web / service / domain / repository) per `CLAUDE.md`.
   - No `switch`/`if` chain on type or status that should be polymorphism (State/Strategy).
   - Inheritance is genuine is-a; otherwise composition.
   - Errors raise a specific `ItsmException` subclass.
3. Confirm the milestone's pattern(s) are present and easy to locate (class names like `*State`, `*Strategy`, `*Factory`, `*Listener`, `*Adapter`).
4. Report a short table: class → OK / issue → fix. Fix trivial issues directly; ask before large refactors.
5. For a deeper review, delegate to the `oop-reviewer` subagent.
