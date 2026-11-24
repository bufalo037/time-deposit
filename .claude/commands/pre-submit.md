---
description: Verify the solution against README.md requirements before submission
---

Audit the repository against `README.md` and report a checklist (✅ / ❌ with evidence: file paths, command
output). Don't fix anything unless asked.

Check:

1. **Build:** `cd java && ./mvnw clean verify` passes (unit, integration/Testcontainers, ArchUnit, OpenAPI
   contract).
2. **Exactly two endpoints:** list every `@*Mapping` in `java/src/main` and every path in `java/openapi.yaml`.
   There must be exactly two business routes (update all balances, list all deposits) and nothing else.
3. **GET schema:** the response has `id`, `planType`, `balance`, `days`, `withdrawals`.
4. **DB schema:** the Flyway migrations define `time_deposits` and `withdrawals` with the required columns,
   types, NOT NULL constraints and FK.
5. **Invariants:**
   - `git diff main -- java/src/main/java/org/ikigaidigital/TimeDeposit.java` shows no breaking change.
   - The `updateBalance(List<TimeDeposit>)` signature is unchanged.
   - The characterization tests are intact.
6. **Interest rules:** the policies match the README (basic 1%, student 3% under 1 year, premium 5% after
   45 days, no interest for the first 30 days).
7. **Architecture:** no Spring/JPA imports in `domain` or the legacy classes.
8. **Docs:**
   - The README explains how to run the app and trigger both endpoints via Swagger UI.
   - `java/openapi.yaml` is committed and current.
   - `docs/AI_USAGE.md` covers tools, setup, custom rules/configs, and which parts were AI-assisted and why.
   - ADRs cover the key decisions and assumptions.
9. **Git hygiene:**
   - Commits are atomic and use Conventional Commits (`git log --oneline main..HEAD`).
   - No secrets or build artifacts are committed.
   - The remote is the personal fork.
   - The other language folders are unchanged (`git diff main --stat -- c# kotlin python typescript`).
