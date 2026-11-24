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

## Agent configuration (committed in this repo)

| File | Purpose |
|---|---|
| [`CLAUDE.md`](../CLAUDE.md) | Repository-level rules, loaded every session. It imports `README.md` as the spec, maps the repo (only `java/` is worked on), and lists the invariants and the git/commit workflow. |
| [`java/CLAUDE.md`](../java/CLAUDE.md) | Loaded when working in `java/`: stack, commands, hexagonal package layout, architecture and testing rules. |
| [`.claude/commands/pre-submit.md`](../.claude/commands/pre-submit.md) | `/pre-submit`: audits the repo against every README requirement and reports pass/fail with evidence. Run before each PR and before submission. |
| [`.claude/commands/new-plan.md`](../.claude/commands/new-plan.md) | `/new-plan <name> <rate> [rules]`: adds a new interest plan test-first, without touching existing code. It shows the interest design is open for extension. |
| [`.claude/skills/adr/SKILL.md`](../.claude/skills/adr/SKILL.md) | `adr` skill: the agent writes an Architecture Decision Record when a change alters the architecture. It asks the human to make the decision if it's still open, and commits the ADR atomically with the implementing code. |
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

## Summary

_To be completed at submission: an overall view of which parts were AI-generated, AI-assisted or hand-written,
and lessons learned._
