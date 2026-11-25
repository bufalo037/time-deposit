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
| 2026-11-25 | Persistence | Using the `flyway-migration` and `writing-tests` skills: planned the schema and asked the human to decide the 4 open questions (naming, money type, plan-type CHECK, seed). Wrote the V1 migration, `Withdrawal` domain record, 3 outgoing ports, package-private JPA entities/repositories/mapper and the persistence adapter, 11 `@DataJpaTest` tests on Testcontainers PostgreSQL, and ADRs 0003 (hexagonal) and 0004 (schema and money). Mutation-checked mapper and adapter: 5/5 caught, after adding a `1004.165` rounding case and an update-then-list ordering case that the first draft missed | Chose snake_case, `numeric(19,2)` with HALF_UP at the boundary, no plan-type CHECK and the proposed seed; reviewed and committed | The schema and money decisions are the human's (recorded in ADR 0004). The agent did the wiring and found that two "obvious" tests didn't guard anything |
| 2026-11-25 | Seed data | Wrote `V2__seed_time_deposits.sql` (6 deposits covering every plan before and after interest starts, 4 withdrawals, identity sequences reset), with "after update" values captured from the real calculator. Added `SeedDataTest`, made the adapter tests start from empty tables, and replaced an order-dependent id assertion (sequences aren't rolled back) with a collision check that is proven to fail without `setval`. Added both lessons to the `writing-tests` skill | Approved the proposed seed set; reviewed and committed | Deterministic demo data for reviewers. The flaky-test pitfall is now written into the skill, so it isn't repeated |
| 2026-11-25 | Use cases | Planned the API branch and asked the human the 4 open decisions (endpoints, update response, Spring in the application layer, money JSON format). Wrote the incoming ports (`ListTimeDepositsUseCase`, `UpdateAllBalancesUseCase`), the `TimeDepositView` read model, `TimeDepositService` (transactional update via the unchanged legacy calculator; list in 2 queries), the calculator bean in `config/`, 7 unit tests using the real calculator (4/4 injected bugs caught), and ADR 0005 | Chose POST + 204, allowed `@Service`/`@Transactional` in application services (overriding the agent's stricter recommendation), and JSON numbers for money | The use-case wiring is mechanical. The architectural trade-off was the human's call and is recorded in ADR 0005 |
| 2026-11-25 | REST API and OpenAPI contract | Added springdoc 3.2.1 (checked it's built on Boot 4.2.0). Wrote the controller (exactly the two README endpoints), response DTOs with OpenAPI annotations, a deterministic spec config (sorted keys, relative server URL), the committed `openapi.yaml`, and 10 tests: `@WebMvcTest` strict JSON, end-to-end on the seed via Testcontainers, and a contract test that also asserts exactly two paths. Fixed a `*/*` response type found by reading the generated contract. Mutation-checked: 5/5 caught (renamed path, wrong status, extra endpoint, unrounded money, renamed field). Smoke-tested Swagger UI and both endpoints on the real app. Drafted ADRs 0006 and 0007 | Chose code-first OpenAPI (at project start), POST + 204 and JSON number money; reviewed and committed | The contract can't drift from the code, and the "exactly two endpoints" rule is enforced by a test, not just documented |
| 2026-11-25 | Architecture tests | Added ArchUnit 1.5.1 (core library with plain JUnit tests, to avoid test-engine compatibility issues with Boot 4's JUnit) and `ArchitectureTest` with 9 rules from ADR 0003/0005: inward dependencies, framework-free domain and legacy classes, web adapter only on `port/in`, adapters independent, application never on adapters, ports are interfaces, entities package-private in the persistence adapter, controllers only in the web adapter. Mutation-checked: 4/4 injected violations caught (Spring in domain, controller → service, Spring on the legacy calculator, public entity) | Asked for the architecture to be enforced, as planned in ADR 0003; reviewed and committed | Turns the hexagonal rules in CLAUDE.md into build failures, which matters most when an AI agent writes much of the code |
| 2026-11-25 | Run guide | Wrote the README "Solution" section (prerequisites, start, step-by-step Swagger UI walkthrough, before/after balances table, curl, tests, links). Checked the claims against the running app first: restart keeps data, `docker compose down` resets to the seed. Updated root `CLAUDE.md` so the agent treats only the assignment part as the spec | Asked for the submission instructions; reviewed and committed | README submission requirement ("clear instructions on how to trigger the endpoints using the Swagger contract"), written from verified behaviour, not assumptions |
| 2026-11-25 | Interest refactor | Planned the Strategy design against SOLID and the README, and asked the human two decisions (grace-period placement, extensibility demo). Probed the legacy arithmetic to pin the operation-order trap. Extracted `InterestPolicy`, three per-plan policies, the `GracePeriod` decorator and the `InterestPolicies` registry. `TimeDepositCalculator` now uses them with an unchanged signature, no-arg constructor and rounding. Added 45 unit tests (including the 30/31 grace boundary that can't be observed for premium through `updateBalance`, and an extra plan picked up by the unchanged calculator), extended the coverage gate and ArchUnit, and drafted ADR 0008. The 72 frozen characterization tests pass unedited, and 13 injected mutations in the new classes are caught by them alone | Chose the `GracePeriod` decorator (README "existing plans") and no throwaway `/new-plan` demo; reviewed and committed | README requirement 4 (extensibility, SOLID) delivered without changing behaviour by a cent, which the safety net written before the refactor proves |
| 2026-11-25 | Dead-code cleanup | Audited for dead code: JaCoCo (143/143 production lines executed), unused imports (none), test-only APIs, and `dependency:analyze`, separating real findings from Spring Boot starter false positives. Verified that removing the unused `testcontainers-junit-jupiter` and `spring-boot-starter-flyway-test` keeps the build identical (161 tests, Flyway still applies both migrations), then removed them together with the placeholder kata test (`assertThat(1).isEqualTo(1)`) | Chose what to remove (the two dependencies and the placeholder test; kept `planTypes()` and the public constants) | A smaller, honest dependency and test surface. Every removal was checked against a green build first |
| 2026-11-25 | Architecture review and summary | Reviewed the architecture (import-based dependency graph, visibility, ArchUnit, SQL counts, dead code) and wrote `docs/ARCHITECTURE_REVIEW.md` with 8 findings. Wrote this file's Summary from the work log and repository facts | Asked for both documents; reviewed and committed | Requirement 6 summary, plus an honest record of known limitations for reviewers |

## Summary

**In one sentence:** Claude Code wrote nearly all code, tests and documentation; the human made every
architectural and business decision, reviewed every diff, and made every commit, behind guardrails that turn the
rules into build failures.

### Who did what

| Area | AI-generated | Human-decided |
|---|---|---|
| Agent harness (`CLAUDE.md` files, 3 skills, 4 commands, Claude hook, git hook, permissions) | Drafted all files and tested every hook (green and failing cases) | The structure (lean root `CLAUDE.md` + module file, ADRs only alongside code), which guardrails to add, and dropping the `Co-Authored-By` trailers |
| Build and infrastructure (Spring Boot 4.2, Maven wrapper with pinned checksum, PostgreSQL/Flyway/Testcontainers, JaCoCo, ArchUnit) | Configuration, version research, verification | Java + Spring Boot, PostgreSQL + Testcontainers, the 100% coverage gate on the interest logic |
| Characterization tests (72) | Captured expected values by running the original code; mutation-checked | Scope (invalid input left unpinned) |
| Persistence (schema, adapter, seed) | Migrations, entities, adapter, tests | snake_case names, `numeric(19,2)` with HALF_UP at the boundary, no plan-type `CHECK`, the seed set (ADR 0004) |
| API (use cases, controller, OpenAPI contract) | Ports, service, controller, DTOs, contract test | POST + 204, JSON-number money, code-first OpenAPI, allowing Spring in application services (overriding the agent's stricter suggestion; ADR 0005) |
| Interest refactor (Strategy pattern) | Policies, decorator, registry, 45 unit tests | `GracePeriod` as a decorator ("existing plans"); no throwaway `/new-plan` demo |
| Documentation (8 ADRs, README run guide, this file, architecture review) | Drafted everything from verified behaviour | Approved each decision recorded in them |

**Nothing was hand-written line by line.** The human's contribution was the decisions, review, scoping and
corrections: every commit was reviewed with `/commit-check` and committed manually, and all work was merged through
pull requests on the personal fork.

### Why AI, and how it was kept honest

- **Why:** most of the work was mechanical but error-prone (build migration, wiring, boilerplate, exhaustive
  boundary tests). The agent did it fast, while the human kept the judgment calls.
- **Every claim was verified, not assumed.** Expected values come from running the original code. Hooks and gates
  were each proven to fail on purpose. The run guide's statements were checked on the running app.
- **Rules are enforced by the build, not just written down.** A Claude Code pre-commit hook, a git pre-push hook,
  `/commit-check` before every commit, characterization tests, a 100% coverage gate, ArchUnit and an OpenAPI
  contract test. Breaking a rule fails the build. It doesn't rely on the agent remembering.
- **The agent asked; the human decided.** Every open README ambiguity was put to the human with options and a
  recommendation (see the ADRs), and the `adr` and `flyway-migration` skills require this.

### Lessons learned

1. **First drafts of tests often guard nothing. Mutation checks catch that.** Several plausible tests passed for
   the wrong reason: a rounding case on the wrong side of the binary half-cent, an ordering test PostgreSQL passed by
   accident, an order-dependent id assertion, and a missed `BigDecimal.valueOf` swap. Each was found only by
   deliberately breaking the code, and each lesson went back into the `writing-tests` skill.
2. **Pin legacy behaviour by running it, not by re-deriving it.** Double-precision artifacts (`1000.9300000000001`)
   and the operation order `(balance * rate) / 12` would have been "fixed" by hand-calculated expectations.
3. **Make the harness self-improving.** Skills, rules and checks were refined whenever a mistake surfaced, so the same
   mistake isn't repeated. Examples: never assert generated ids, delete seed rows in database tests, lowercase branch
   names.
4. **Watch the environment, not just the code.** VS Code and Maven writing to the same `target/classes` caused two
   false failures (see [ARCHITECTURE_REVIEW.md](ARCHITECTURE_REVIEW.md)). An agent that checks timestamps and logs
   finds such issues quickly. One that only reruns the build would have chased phantom bugs.
5. **Keep the human in the loop where it matters.** The human overrode the agent once (Spring in application
   services), declined optional extras, and caught a confusing manual test result. That led to verifying persistence
   on the live database and finding a real test gap (the end-to-end test never proves a committed update).

Open follow-ups are listed in [ARCHITECTURE_REVIEW.md](ARCHITECTURE_REVIEW.md) (findings F1–F8).
