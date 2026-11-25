# Architecture review

**Scope:** the complete solution in `java/` at `main` @ `fc9c664` (after PR #6). **Date:** 2026-11-25.
**Checked against:** the README requirements and preferred stack, and ADRs [0001–0008](adr/).
**Method:** dependency graph from the actual imports, class visibility, the ArchUnit rules, a full build
(`./mvnw clean verify`), SQL statement counts on a running instance, and a dead-code audit.

**Verdict: compliant.** The hexagonal architecture, SOLID, the design patterns and every README constraint hold, and
the important rules are enforced by the build. No violations were found. Four medium findings remain open (below).

## 1. Dependency structure

```
adapter.in.web ──► application.port.in ──► domain
      │                                      ▲
      └──────────────────────────────────────┘ (Withdrawal)
adapter.out.persistence ──► application.port.out ──► domain, legacy (TimeDeposit)
application.service ──► port.in, port.out, domain, legacy (TimeDeposit, TimeDepositCalculator)
config ──► legacy (TimeDepositCalculator)
legacy TimeDepositCalculator ──► domain.interest ──► legacy TimeDeposit
```

- Every dependency points inward. Nothing depends on an adapter, and the two adapters don't depend on each other.
- All 10 adapter types (controller, response DTOs, JPA entities, Spring Data repositories, mapper, persistence
  adapter) are **package-private**, so the compiler stops anything outside the adapter from using them.
- Framework-free: no Spring, JPA, Hibernate, Jackson or Swagger imports in `domain/` or the legacy classes.
  Spring annotations appear in `application/service` only, as ADR 0005 allows.

## 2. Compliance with the README

| Requirement | Status | Evidence |
|---|---|---|
| Hexagonal architecture | ✅ | `adapter.in`/`adapter.out` behind `port.in`/`port.out`. ADR 0003/0005, enforced by 10 ArchUnit rules (`ArchitectureTest`). |
| SOLID | ✅ | **SRP:** one policy per plan, the grace period on its own, registry, calculator, mapper and DTOs each do one job. **OCP:** a new plan is a new class (`InterestPoliciesTest` proves it). **LSP:** a shared `InterestPolicy` contract. **ISP:** 3 outgoing ports, 2 use cases. **DIP:** the service depends on ports, the calculator on `InterestPolicy` (ADR 0008). |
| Design patterns | ✅ | Strategy (`InterestPolicy`), Decorator (`GracePeriod`), Registry (`InterestPolicies`), Ports & Adapters, Repository, a read model (`TimeDepositView`), mapper/DTO separation |
| Extensible interest calculation | ✅ | ADR 0008. Adding a plan doesn't change the calculator or existing policies. |
| `TimeDeposit` unchanged, `updateBalance` signature unchanged | ✅ | `TimeDeposit.java` is byte-identical to the original kata. The calculator only gained constructors. 72 frozen characterization tests pass. |
| `updateBalance` behaviour unchanged | ✅ | Characterization tests mutation-checked (every boundary in both directions, rounding, operation order). A JaCoCo gate requires 100% line and branch coverage of the calculator and `domain.interest`. |
| Exactly two endpoints | ✅ | `GET /time-deposits`, `POST /time-deposits/balances/update`. Asserted by `OpenApiContractTest`. |
| GET schema `id, planType, balance, days, withdrawals` | ✅ | Checked by a strict JSON test and the committed `openapi.yaml` |
| Database tables | ✅ | Flyway V1 with every required column `NOT NULL` and the FK (ADR 0004) |
| OpenAPI Swagger contract | ✅ | Generated from the code, committed and checked by a test (ADR 0006). The README explains how to use Swagger UI. |
| Testcontainers | ✅ | Real PostgreSQL in 5 test classes (ADR 0002) |
| Clean code | ✅ (with nits) | Small classes, no dead code (143/143 production lines covered, no unused imports or dependencies), named constants. See the nits below. |

## 3. Findings

| # | Severity | Finding | Recommendation |
|---|---|---|---|
| F1 | **Medium** | **Concurrent updates can lose interest.** There's no optimistic or pessimistic locking. Two simultaneous `POST /balances/update` calls read the same balances and both write one month's interest, so a month is lost. | Optimistic locking: a `version` column (new migration V3) plus `@Version`, or a row lock in the adapter. Record it in an ADR. |
| F2 | **Medium** | **Seed data runs in every environment.** `V2__seed_time_deposits.sql` sits next to the schema migrations, so a production database would get the demo deposits. | Move it to `db/seed/` and include that folder only in the default/dev configuration. |
| F3 | **Medium** | **The root package and `domain.interest` depend on each other.** `TimeDepositCalculator` (root) uses `InterestPolicies`, and the policies use `TimeDeposit` (root). There's no cycle between classes. It exists because the README forbids moving the legacy classes out of `org.ikigaidigital`. | Record it as an accepted consequence, and add an ArchUnit no-package-cycles rule that exempts exactly this pair. |
| F4 | **Medium** | **ArchUnit gaps.** The layered rule ignores the root package (where the legacy classes live). There's no package-cycle check, and nothing says outgoing ports are implemented only in `adapter.out`. All are true today, but not enforced. | Add 2–3 rules. |
| F5 | Low | **Write-path efficiency.** The update sends one `UPDATE` per changed deposit, without JDBC batching, plus one redundant re-select. Measured: GET = 2 statements, POST = 2 selects + 1 update per changed row. There's no N+1 in the reads. The whole table is held in memory during an update. | Enable `hibernate.jdbc.batch_size` and `order_updates`, add a test that counts statements, and process in chunks for very large tables. |
| F6 | Nit | **Legacy names** `xs` and `a2d` remain in `TimeDepositCalculator`. | Rename them (`timeDeposits`, `newBalance`). Parameter names aren't part of the signature, so this doesn't break the README constraint. |
| F7 | Nit | **`TimeDepositService` implements both use cases.** | Keep it. Splitting it would be over-engineering at this size. |
| F8 | Nit | **Money conversion** (`valueOf(…).setScale(2, HALF_UP)`) **is repeated in both adapters.** | Keep it. Intentional, so the adapters stay independent (ADR 0004/0007). |

## 4. Recommended order

1. **F1 and F2:** the most likely reviewer questions (correctness under concurrency, environment hygiene).
2. **F3 and F4:** document the accepted package cycle and close the ArchUnit gaps.
3. **F5 and F6:** optional performance and readability improvements.

## 5. Notes outside the architecture

- **Development environment:** VS Code's Java extension and Maven both write to `java/target/classes`. While one is
  rebuilding, the other can see a half-written set of classes. This caused a false JaCoCo "0%" result and a
  `NoClassDefFoundError` at startup during the review. It doesn't affect the code. Avoid running `./mvnw` while the
  IDE rebuilds, or set `"java.autobuild.enabled": false` locally. The git pre-push hook isn't affected, because it
  builds in an isolated worktree.
- **Reviewer tip:** only seeded deposits 2, 3 and 6 change on `POST /balances/update`. Deposits 1, 4 and 5 sit
  outside their interest window by design, and the update doesn't advance `days` (ADR 0007).
