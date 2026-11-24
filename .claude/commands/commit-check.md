---
description: Check the staged changes against the repository rules before committing, then draft the commit
argument-hint: [optional intent of the commit]
---

Check the **staged** changes against the rules in `CLAUDE.md`, `java/CLAUDE.md` and `README.md`. This is the
per-commit subset of `/pre-submit`: every commit that passes this check keeps the branch compliant.
Commit intent (optional): $ARGUMENTS

**Don't commit and don't change the staging area.** The user commits manually. Only fix files if the user asks.

## 1. Collect the facts

Run and read:
- `git diff --cached --stat` and `git diff --cached` (what will be committed)
- `git status --short` (unstaged/untracked leftovers)
- `git log --oneline -5` (style of previous commits)

If nothing is staged, say so and stop.

## 2. Check each rule

Report each item as ✅ / ❌ / ➖ (not applicable), with evidence (file path, diff hunk or command output):

1. **Atomic:** the staged files form **one** logical change. Name it in one sentence. If they mix concerns (e.g. a
   feature plus unrelated refactor/docs/config), ❌ and propose how to split them (which files go in which commit).
2. **Complete:** nothing the change needs is left unstaged or untracked. Examples: a new class without its test, a
   pom change without the code that uses it, an ADR without its index row. ❌ with the exact `git add` to fix it.
3. **Build green on what's staged:** if `java/` has unstaged or untracked changes, ⚠️ warn that `./mvnw verify`
   tests the working tree, not the staged snapshot. Then run `cd java && ./mvnw -q -B clean verify` and report
   the test counts.
4. **Invariants** (`CLAUDE.md`):
   - `TimeDeposit.java`: no breaking change in the staged diff (package, constructor, accessors, field types).
   - `TimeDepositCalculator.updateBalance(List<TimeDeposit>)`: signature unchanged.
   - The characterization tests aren't deleted, weakened, or changed in their expected values.
   - `c#/`, `kotlin/`, `python/`, `typescript/` aren't touched.
5. **Endpoints:** count every `@*Mapping` in `java/src/main` after this commit. There are at most two business
   routes (update all balances, list all deposits), with no new ones beyond those.
6. **Architecture** (`java/CLAUDE.md`): staged files in `domain/` or the legacy classes don't import Spring, JPA,
   Jackson or springdoc. Adapters stay in `adapter/`, and controllers depend only on `port/in`.
7. **ADR:** if the change alters the architecture (new layer, port/adapter, framework/library, pattern, schema
   design, API contract shape, a README ambiguity resolved), an ADR from the `adr` skill **and** its index row
   are staged. If one is missing, ❌ and offer to write it.
8. **Contract:** if controllers/DTOs changed, `java/openapi.yaml` is regenerated and staged in the same commit.
9. **Migrations:** Flyway migrations already on `main` or in earlier commits are **never edited**. Schema changes
   are a new `V<n>__*.sql`.
10. **Docs in sync:** if a command, rule or structure changed, `CLAUDE.md` / `java/CLAUDE.md` are updated in the
    same commit. If the work was AI-assisted, `docs/AI_USAGE.md` has a work-log row.
11. **Hygiene:** no secrets or credentials (other than documented local-only dev values), no build output
    (`target/`), no IDE/personal files (`.idea/`, `*.iml`, `.claude/settings.local.json`), no stray debug code
    or `TODO` without context.

## 3. Verdict and commit draft

- One line: **Ready to commit**, or **Not ready**, followed by the ❌ items and their fixes.
- When ready (or once the fixes are listed), draft a commit message:
  - Conventional Commits: `type(scope?): summary`, imperative, ≤ 72 chars, no trailing period. Pick the type
    from the change (`feat`, `fix`, `refactor`, `test`, `build`, `ci`, `docs`, `chore`).
  - Body: short bullets saying *what* and *why*, mentioning any ADR added.
  - No `Co-Authored-By` trailer unless the user asks for one.
- Give it ready to paste, followed by the check:

```bash
git commit -F - <<'EOF'
<type>: <summary>

- <bullet>
EOF

git log -1 --stat
```
