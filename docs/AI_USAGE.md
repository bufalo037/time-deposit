# AI-Assisted Development

How AI tooling was set up and used for this take-home (README requirement 6).

## Tools

| Tool | Version / model | Role |
|---|---|---|
| Claude Code (VS Code extension) | Claude Opus 5.5 (`claude-opus-5-5`) | Main coding agent: planning, code generation, tests, refactoring, docs |

## Setup (reproducible)

1. Install [Claude Code](https://docs.claude.com/en/docs/claude-code) (CLI or VS Code extension) and sign in.
2. Clone this repository and open it at the repository root. Claude Code picks up the configuration below
   automatically. Nothing else needs installing for the agent itself.
3. Prerequisites for the code the agent builds and tests: Java 21 and Docker (for Testcontainers and local
   PostgreSQL).
4. Enable the repository's git hooks once: `git config core.hooksPath .githooks`.

## Agent configuration (committed in this repo)

| File | Purpose |
|---|---|
| [`CLAUDE.md`](../CLAUDE.md) | Repository-level rules, loaded every session. It imports `README.md` as the spec, maps the repo (only `java/` is worked on), and lists the invariants and the git/commit workflow. |
| [`java/CLAUDE.md`](../java/CLAUDE.md) | Loaded when working in `java/`: stack, commands, hexagonal package layout, architecture and testing rules. |
| [`.claude/settings.json`](../.claude/settings.json) | Shared permissions. Build, `docker compose`, and read-only/staging git commands are allowed. `git commit`/`git push` always ask. Force-push, `reset --hard` and edits to the reference language folders are denied. It also registers the pre-commit hook below. |
| [`.claude/hooks/verify-before-commit.sh`](../.claude/hooks/verify-before-commit.sh) | `PreToolUse` hook: before Claude runs any `git commit`, it runs `./mvnw verify` and **blocks the commit** (exit 2, with the failing output fed back to Claude) if the build is red. It turns "build green at every commit" from a written rule into an enforced one. |
| [`.githooks/pre-push`](../.githooks/pre-push) | Plain git hook (not Claude-specific, enabled with `git config core.hooksPath .githooks`). Before **any** push, by a human or the agent, it checks out each pushed commit into a temporary worktree and runs `./mvnw verify`, aborting the push on failure. Only tested code reaches the remote. |
| [`.claude/commands/commit-check.md`](../.claude/commands/commit-check.md) | `/commit-check`: before each manual commit, audits the **staged** changes against the repo rules (atomic, complete, build green, invariants, endpoint count, architecture, ADR, contract, migrations, docs, hygiene) and drafts a Conventional Commits message. It's the per-commit subset of `/pre-submit`, so compliance is kept commit by commit instead of being discovered at the end. |
| [`.claude/commands/pr-description.md`](../.claude/commands/pr-description.md) | `/pr-description [base]`: reads the branch's commits, ADRs and AI work-log rows, and writes a ready-to-paste PR/MR description (summary, changes grouped by commit type, decisions, how to test, README compliance, AI assistance) plus a title. Read-only. |
| [`.claude/commands/pre-submit.md`](../.claude/commands/pre-submit.md) | `/pre-submit`: audits the repo against every README requirement and reports pass/fail with evidence. Run before each PR and before submission. |
| [`.claude/commands/new-plan.md`](../.claude/commands/new-plan.md) | `/new-plan <name> <rate> [rules]`: adds a new interest plan test-first, without touching existing code. It shows the interest design is open for extension. |
| [`.claude/skills/adr/SKILL.md`](../.claude/skills/adr/SKILL.md) | `adr` skill: the agent writes an Architecture Decision Record when a change alters the architecture. It asks the human to make the decision if it's still open, and commits the ADR atomically with the implementing code. |
| [`.claude/skills/flyway-migration/SKILL.md`](../.claude/skills/flyway-migration/SKILL.md) | `flyway-migration` skill: the agent writes PostgreSQL Flyway migrations to a fixed standard (naming/versioning, named constraints, indexed FKs, exact numeric types, no edits to committed migrations), asks the human about open schema design questions, and verifies against Testcontainers. |
| [`.claude/skills/writing-tests/SKILL.md`](../.claude/skills/writing-tests/SKILL.md) | `writing-tests` skill: picks the cheapest test type (characterization, unit, `@DataJpaTest`/`@WebMvcTest` slices, end-to-end, ArchUnit, contract), enforces conventions (AssertJ, parameterized boundaries, exact money values, test-first) and freezes characterization tests once committed. |
| [`docs/adr/`](adr/) | Architecture Decision Records. The agent reads them for the reasons behind the rules in `CLAUDE.md`. |

`.claude/settings.local.json` (personal permissions) is git-ignored on purpose.

### Design principles behind the configuration

- **Keep CLAUDE.md lean and lasting.** It holds only what's true for the codebase's whole life: structure,
  commands, invariants. One-off tasks live in slash commands, and the reasoning lives in ADRs.
- **README is the single source of truth.** It's imported, not paraphrased, so the rules can't drift from it.
- **Invariants are backed by tests, not just prose.** Characterization tests guard `updateBalance`, ArchUnit
  guards the layering, and a contract test guards `openapi.yaml`, so the agent gets a failing build instead of
  having to remember a rule.
- **Human in the loop.** The agent proposes and a human reviews every diff. Commits and pushes only happen on
  explicit request.

## AI-assisted work log

Which parts of the solution were AI-assisted, and why. Updated as work progresses.

| Date | Area | What the AI did | What the human did | Why AI was used |
|---|---|---|---|---|
| 2026-11-24 | Project setup | Analysed the README and repo, then proposed and drafted `CLAUDE.md`, `java/CLAUDE.md`, slash commands, this file and the `adr` skill | Chose the stack (Java, PostgreSQL, Testcontainers, code-first OpenAPI), reviewed and steered the CLAUDE.md structure (split into root and module files) | Fast first draft of the agent configuration and decision records, with the human making every architectural choice |
| 2026-11-24 | Build setup | Generated a reference project with Spring Initializr, added the Maven wrapper, migrated `pom.xml` to Spring Boot 4.2 / Java 21, added a context smoke test, ran the build, and drafted ADR 0001 with the `adr` skill | Chose Java + Spring Boot, reviewed the diff and committed | Boilerplate build migration is mechanical. The agent also caught the Initializr `.RELEASE` version mismatch by running the build |
| 2026-11-24 | Agent harness | Wrote `.claude/settings.json` (permissions) and the pre-commit build hook, and tested it: non-commit commands pass through, a green build allows the commit, and a temporary failing test blocks it | Asked for the hook, reviewed the permission lists and committed | Hooks and permissions are what make agent rules enforced instead of advisory |
| 2026-11-24 | Database infrastructure | Added JPA/Flyway/PostgreSQL/Testcontainers dependencies, `compose.yaml` with a random host port (after finding 8080/8081 taken locally), a shared Testcontainers config and a test-run entry point. Verified both the test path and the `spring-boot:run` + Compose path against PostgreSQL 18.7, and drafted ADR 0002 | Chose PostgreSQL + Testcontainers, reviewed and committed | Wiring infrastructure is mechanical and easy to verify end to end |
| 2026-11-25 | Agent harness | Drafted the `/commit-check` command, deriving its checks from `/pre-submit` and the CLAUDE.md rules | Asked for a per-commit compliance gate, since commits are made manually and skip the Claude Code hook | Catches rule violations at commit time instead of at submission |
| 2026-11-25 | Git hooks | Wrote `.githooks/pre-push`, which builds each pushed commit in an isolated worktree, and tested it: green push allowed in ~22s, branch delete skipped, and a commit with a failing test aborted (in a throwaway clone) | Asked for test validation before every push, including manual ones | The Claude Code hook only covers agent commits. A git hook protects the remote for every push |
| 2026-11-25 | Agent harness | Drafted the `/pr-description` command | Asked for a copy-paste PR/MR summary of each branch | Consistent, evidence-based PR descriptions, with no hand-written commit recaps |
| 2026-11-25 | Agent harness | Drafted the `flyway-migration` skill, based on ADR 0002 and the README schema | Asked for a migration-writing skill ahead of the persistence feature | Keeps schema work consistent and append-only before any table exists |
| 2026-11-25 | Agent harness | Drafted the `writing-tests` skill and corrected the `./mvnw test` description in `java/CLAUDE.md` (it also runs Docker-backed tests) | Asked for a test-writing skill before the characterization tests | Consistent test types and conventions; characterization tests protected from "fixes" |
| 2026-11-25 | Characterization tests | Using the `writing-tests` skill: probed the original `updateBalance` to capture its real outputs, wrote 72 parameterized characterization cases (grace period, every plan boundary, unknown and case-variant plans, HALF_UP rounding on exact binary values, double artifacts, large balances, batch, compounding, side effects), and mutation-checked them. 10/10 injected bugs are caught, including a `new BigDecimal(double)` → `BigDecimal.valueOf` swap that the first draft missed | Asked for tests that lock the logic and cover edge cases, and reviewed the scope (invalid input left unpinned, per README) | Expected values captured from the real code can't be wrong by hand-calculation, and mutation checks prove the tests actually guard behaviour |
| 2026-11-25 | Test coverage | Added JaCoCo 0.8.16 (report on every `verify`) and a gate requiring 100% line and branch coverage of `TimeDepositCalculator`. Measured 15/15 lines and 14/14 branches with the characterization tests, versus 73% lines and 42% branches with only the original kata test (the gate fails the build in that case). Updated `java/CLAUDE.md` and the `writing-tests` skill | Asked to see how well the logic is covered in this MR | Turns "the logic is fully tested" into a measured and enforced fact, also run by the pre-push hook |

## Summary

_To be completed at submission: an overall view of which parts were AI-generated, AI-assisted or hand-written,
and lessons learned._
