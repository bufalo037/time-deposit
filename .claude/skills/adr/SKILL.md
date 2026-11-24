---
name: adr
description: Write an Architecture Decision Record in docs/adr/ when a change alters the architecture. Use it whenever the current work introduces or changes a layer, module, port/adapter, framework or library, design pattern, persistence/schema design, API contract shape, or a cross-cutting concern. Also use it when a README ambiguity is resolved in a way that shapes the design, or when existing code deviates from or replaces an existing ADR. Don't use it for bug fixes, renames, or refactors that follow an already-recorded decision.
---

# Writing an ADR

An ADR records **one** architectural decision: its context, the decision, and the consequences. It's committed
**in the same commit as the code that implements it**, never ahead of it.

## 1. Decide whether an ADR is warranted

Write one if the change:
- adds or changes a layer, module, port, adapter or package boundary
- introduces a framework, library or infrastructure component (database, migration tool, container, codegen…)
- picks a design pattern that shapes how future code is written (e.g. strategy for interest)
- defines or changes persistence/schema design or the API contract (paths, methods, payload shape)
- resolves a README ambiguity in a way other code will depend on
- contradicts or replaces an existing ADR

Skip it for bug fixes, renames, local refactors and tests that follow existing decisions. Use a code comment for
small, local assumptions instead (README: "justify them in code comments").

If unsure, ask the user before writing.

## 2. Gather context

1. Read `README.md` and quote the requirement(s) that drive the decision.
2. Read `docs/adr/README.md` and the existing ADRs. Don't duplicate a recorded decision. If the new decision
   replaces one, this is a **superseding** ADR (see step 5).
3. Look at the actual code or diff being introduced. The ADR must describe what the code does, not an ideal.
4. If the decision itself hasn't been made explicitly by the user, present 2–3 options with a recommendation and
   **ask the user to choose** before writing. The ADR records the human's decision. It doesn't make one.

## 3. Write the file

- Number: the next free 4-digit number in `docs/adr/` (`0001`, `0002`, …).
- File name: `docs/adr/NNNN-kebab-case-title.md`.
- Follow the template in `docs/adr/README.md`:

```markdown
# NNNN. Title

## Status
Accepted (YYYY-MM-DD)

## Context
The forces at play: README requirement(s) (quoted), constraints (invariants in CLAUDE.md), current state of the code.

## Decision
What we do, concretely: names of packages, classes, tables, endpoints.
Mark every assumption explicitly as *Assumption:* with its justification.

## Consequences
Positive and negative. The trade-offs we accept. What becomes easier or harder later.
Rejected alternatives and why, in one line each.
```

Style rules:
- Under a page. Plain, specific language. No marketing words.
- Link to code paths or related ADRs instead of repeating them.
- Use today's date in absolute form.

## 4. Update the index

Add a row to the table in `docs/adr/README.md`:

```markdown
| [NNNN](NNNN-kebab-case-title.md) | Short decision summary | Accepted |
```

## 5. Superseding an existing ADR

Never rewrite an accepted ADR's decision. Instead:
- Write a new ADR that explains what changed and why.
- In the old ADR, change only the Status to `Superseded by NNNN (YYYY-MM-DD)`, and update its row in the index.

## 6. Commit with the implementation

- Stage the ADR and the index change **together with** the implementing code, as one atomic commit.
- Don't make a separate `docs:` commit for an ADR unless the user asks. Don't commit unless the user asks.
- If `CLAUDE.md` or `java/CLAUDE.md` should now state a new rule that follows from the decision, update it in the
  same commit (the rule goes in CLAUDE.md, the reasoning in the ADR).
- If the ADR was AI-drafted, add a line to the work log in `docs/AI_USAGE.md`.
