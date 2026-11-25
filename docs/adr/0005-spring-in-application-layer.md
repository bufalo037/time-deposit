# 0005. Spring annotations allowed in application services

## Status
Accepted (2026-11-25). Refines [ADR 0003](0003-hexagonal-architecture.md).

## Context
ADR 0003 fixes the dependency direction (`adapter → application → domain`) and keeps the domain framework-free. It
doesn't say whether application services may use Spring. The "update all balances" use case must load, calculate
and save atomically, so it needs a transaction boundary. Options were a framework-free service wired in `config/`
with a `TransactionTemplate`, or Spring's `@Service`/`@Transactional` directly on the service.

## Decision
- Application services (`application/service/`) **may** use Spring stereotype and transaction annotations
  (`@Service`, `@Transactional`). `TimeDepositService` implements both incoming ports and runs
  `updateAllBalances()` in one `@Transactional` method (load → `TimeDepositCalculator.updateBalance` → save).
  `listTimeDeposits()` is `@Transactional(readOnly = true)`.
- `application/` still depends only on its ports, the domain and the legacy classes, never on adapters.
- The **domain** (`domain/`) and the **legacy classes** (`TimeDeposit`, `TimeDepositCalculator`) stay
  framework-free. Framework-free classes become beans through `config/` (e.g. `TimeDepositConfiguration` creates the
  `TimeDepositCalculator` bean).
- The planned ArchUnit rules forbid Spring imports in `domain/` and the legacy classes only, and enforce the
  dependency direction for all layers.

## Consequences
- Less wiring code. The transaction boundary is visible exactly where the use case is defined.
- Application services are still unit-testable without Spring (`TimeDepositServiceTest` constructs the service
  directly with mocked ports), because the annotations only take effect inside a Spring context.
- The application layer is coupled to Spring's annotations. Replacing Spring would mean touching services, but no
  business rules.
- Rejected: framework-free services plus `TransactionTemplate` wiring in `config/`. It's purer, but more
  indirection than this codebase needs.
