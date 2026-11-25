# 0003. Hexagonal architecture: ports and adapters in one module

## Status
Accepted (2026-11-25)

## Context
README: "Embrace Hexagonal Architecture" and "Adhere to SOLID principles". The legacy domain classes
(`TimeDeposit`, `TimeDepositCalculator`) are plain Java and must not change in breaking ways. This change adds the
first infrastructure (persistence), so the dependency direction has to be fixed now, before any web or use-case
code exists.

## Decision
Ports and adapters inside the single `java/` Maven module (layout in `java/CLAUDE.md`):

- `org.ikigaidigital`: legacy `TimeDeposit` / `TimeDepositCalculator`, left in their original package (moving them
  would break consumers). They're treated as domain.
- `domain/`: pure Java domain types, e.g. `Withdrawal` (a record). No framework imports.
- `application/port/out/`: outgoing ports, split by need (Interface Segregation): `LoadTimeDepositsPort`,
  `SaveTimeDepositsPort`, `LoadWithdrawalsPort`. Incoming ports and services come with the endpoints.
- `adapter/out/persistence/`: `TimeDepositPersistenceAdapter` implements the outgoing ports with Spring Data JPA.
  JPA entities, repositories and the mapper are **package-private**, so nothing outside the adapter can reach them.
- Dependencies point inward only: `adapter → application → domain`.

## Consequences
- Domain and application code can be tested without Spring or a database. The database technology can change
  without touching business rules.
- Package-private persistence types make "entities never leave the adapter" a compile-time guarantee, not just a
  convention.
- A single module keeps the build simple. Compile-time module boundaries (multi-module) would be over-engineering
  for this size. An ArchUnit test to enforce the inward dependency rule is planned.
- There are more types (ports, entities, mapper) than in a layered CRUD app. That's accepted as the point of the
  exercise.
- Rejected: classic layered architecture (controller → service → repository with entities everywhere), because
  the domain would depend on JPA.
