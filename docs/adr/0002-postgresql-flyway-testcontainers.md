# 0002. PostgreSQL + Flyway + Testcontainers, Docker Compose for local runs

## Status
Accepted (2026-11-24)

## Context
README: "Store all time deposit plans in a database" with the `timeDeposits` and `withdrawals` tables, and the
preferred stack says "Utilize testcontainers". Tests must exercise the same database engine the app runs on.
Reviewers must be able to run the app with minimal setup. On the development machine, common ports (8080, 8081)
are already taken by other services.

## Decision
- **PostgreSQL 18** (`postgres:18-alpine`, pinned major version) is the only database. There's no H2 or other
  in-memory substitute.
- **Flyway** owns the schema (`src/main/resources/db/migration`). Hibernate runs with `ddl-auto=validate`, so a
  mismatch between entities and migrations fails at startup instead of being silently "fixed".
  `open-in-view` is off.
- **Tests:** `PostgresTestcontainersConfiguration` provides a `PostgreSQLContainer` as a `@ServiceConnection`
  bean. Integration tests `@Import` it. Spring's context cache reuses one container across test classes with the
  same configuration.
- **Local runs:** `java/compose.yaml` defines the database, and `spring-boot-docker-compose` starts it on
  `./mvnw spring-boot:run` and wires the datasource automatically. The container publishes PostgreSQL on a
  **random free host port** to avoid clashes. `./mvnw spring-boot:test-run` (`TestTimeDepositApplication`) runs
  the app on a throwaway Testcontainers database instead.
- No connection settings in `application.properties`. Both paths inject them. The compose credentials are
  local-development values, not secrets.
- The tables themselves are created by a migration that arrives with the persistence adapter. That's where the
  naming and column-type assumptions are decided.

## Consequences
- Integration tests catch real SQL and dialect issues that an in-memory database would hide.
- One command runs the app locally. Reviewers don't have to start the database by hand.
- Docker is required for integration tests (and so for `./mvnw verify` and the pre-commit hook) and for running
  locally. The first run pulls the image (~114 MB).
- The image tag lives in two places (`compose.yaml`, `PostgresTestcontainersConfiguration`). Comments in both
  point at each other.
- Rejected: H2 (a different engine from production, so tests prove less), and `ddl-auto=update` (an unversioned,
  non-reproducible schema).
