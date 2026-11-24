---
description: Summarise the current branch's commits into a ready-to-paste PR/MR description
argument-hint: [base branch, default main]
---

Write a pull/merge request description for the current branch, for the user to paste into the PR/MR
description field. Base branch: `$ARGUMENTS` (use `main` if empty).

**Read-only:** don't commit, push, open a PR or change files.

## 1. Collect the facts

Run and read:
- `git branch --show-current`
- `git log --reverse --format='%h %s%n%b' <base>..HEAD` (every commit, oldest first, with bodies)
- `git diff --stat <base>...HEAD` (files changed overall)
- `git diff --name-status <base>...HEAD -- docs/adr` (ADRs added/changed)
- `git status --short`

If there are no commits ahead of the base, say so and stop. If there are uncommitted changes, warn that they're
**not** included in the description.

Read the added ADRs and the `docs/AI_USAGE.md` work-log rows added on this branch
(`git diff <base>...HEAD -- docs/AI_USAGE.md`), so decisions and AI assistance are described accurately. Base
everything on the commits and diffs. Don't invent changes, test results or numbers.

## 2. Write the description

Output **one** fenced block using `~~~markdown` (tildes, so inner code fences survive), containing:

```
## Summary
2–4 sentences: what this branch delivers and why, in reviewer terms (not a commit list).

## Changes
Grouped by Conventional Commit type, in this order: Features, Fixes, Refactoring, Tests, Build, CI, Docs, Chores
(skip empty groups). One bullet per commit, rewritten as a clear outcome, with the short hash at the end:
- Migrate the Java module to Spring Boot 4.2 / Java 21 with the Maven wrapper (`85280df`)

## Architecture decisions
One bullet per ADR added/changed on this branch: [ADR NNNN – title](docs/adr/NNNN-....md), plus a one-line
decision. Omit the section if there are none.

## How to test
Concrete commands for this branch, taken from `java/CLAUDE.md` (e.g. `cd java && ./mvnw clean verify`). Add the
run/Swagger steps only if the branch adds endpoints. Mention prerequisites (JDK 21, Docker).

## README compliance
Checkboxes only for the README constraints this branch touches or could affect, e.g.:
- [x] `TimeDeposit` / `updateBalance` signature unchanged
- [x] No endpoints beyond the two allowed
Tick a box only with evidence from the diff. Leave it unticked with a short note if unknown.

## AI assistance
2–3 bullets summarising this branch's `docs/AI_USAGE.md` work-log rows: what the AI did, what the human
decided. Link to `docs/AI_USAGE.md`.
```

Rules:
- Be concise and factual, written for a reviewer who hasn't followed the work.
- Use repo-relative links (e.g. `docs/adr/0002-...md`) so they resolve in the PR view.
- No `Co-Authored-By` lines or tool footers unless the user asks for them.

## 3. After the block

Give a suggested PR **title** (Conventional Commits style, ≤ 72 chars) on one line, then stop.
