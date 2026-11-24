# CLAUDE.md

XA Bank time-deposit refactoring kata. The assignment below is the **source of truth**:

@README.md

## Repository map

- `java/`: **the solution. All code changes happen here.** It has its own `java/CLAUDE.md` with stack,
  commands and architecture.
- `c#/`, `kotlin/`, `python/`, `typescript/`: original kata starters, kept for reference only.
  Never modify, build or read them.
- `docs/adr/`: architecture decision records (the *why* behind the rules in these files).
- `docs/AI_USAGE.md`: log of AI tooling and AI-assisted work (README requirement 6).
- `.claude/`: project agent configuration (settings, slash commands).

## Invariants (never violate)

1. `TimeDeposit` is a shared class. Make no breaking changes: keep its package, constructor, accessors and field types.
2. `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` keeps its signature and output to the cent. The
   characterization tests guard it. Never weaken or delete them. If they fail, the change is wrong.
3. Exactly **two** API endpoints. Add no other routes (no actuator, health or CRUD). Swagger UI and the spec are
   documentation, so they're allowed.
4. Don't validate input or handle exceptions (the README says it's not required).
5. For ambiguity, pick a logical assumption and justify it in a code comment. Put significant ones in an ADR.

## Workflow

- Atomic commits: one logical change each, with the build green at every commit (a `PreToolUse` hook in
  `.claude/settings.json` runs `./mvnw verify` and blocks `git commit` if it fails), using Conventional Commits
  (`feat:`, `fix:`, `refactor:`, `test:`, `build:`, `ci:`, `docs:`, `chore:`).
- Branches: `chore/*`, `feat/*`, `refactor/*`, `docs/*`. Never commit directly to `main`.
- Commit or push only when the user asks. Work only in the fork (`origin`). Never push to or open PRs against
  `ikigai-digital/*`.
- When a change alters the architecture, use the `adr` skill to write an ADR. Commit it together with the change
  that implements it, never ahead of the code.
- After meaningful AI-assisted work, add an entry to `docs/AI_USAGE.md`: what, which tool, and why.
- Slash commands: `/commit-check` checks the staged changes against these rules and drafts the commit message.
  `/new-plan <name>` scaffolds a new interest plan. `/pre-submit` checks submission readiness.
