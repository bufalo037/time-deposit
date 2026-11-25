# java/CLAUDE.md

Spring Boot service for the time-deposit kata. Run every command from this directory.

## Stack

- Java 21, Maven via the wrapper `./mvnw` (Maven isn't installed globally)
- Spring Boot (Web, Data JPA), Flyway, PostgreSQL
- springdoc-openapi, **code-first**: the spec is generated from annotated controllers
- JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL), ArchUnit
- Docker must be running for integration tests and the local DB

## Commands

```bash
./mvnw clean verify                 # full build: all tests + JaCoCo coverage report and gate
                                    #   report: target/site/jacoco/index.html (CSV: target/site/jacoco/jacoco.csv)
./mvnw test                         # all tests (Docker needed for Spring/Testcontainers tests)
./mvnw test -Dtest=ClassName        # a single test class (pure unit tests need no Docker)
./mvnw spring-boot:run              # app on :8080; auto-starts PostgreSQL from compose.yaml (random host port)
./mvnw spring-boot:test-run         # app on a throwaway Testcontainers PostgreSQL instead
SERVER_PORT=18080 ./mvnw spring-boot:run   # if 8080 is taken
./mvnw verify -Dopenapi.update=true # regenerate the committed openapi.yaml
```

Swagger UI: http://localhost:8080/swagger-ui.html · Spec: http://localhost:8080/v3/api-docs.yaml

## Package structure (hexagonal)

```
org.ikigaidigital
├── TimeDeposit, TimeDepositCalculator   # legacy shared classes: stay in this package
├── domain/                              # pure Java, no framework imports
│   └── interest/                        # InterestPolicy + one class per plan + registry
├── application/
│   ├── port/in/                         # use-case interfaces
│   ├── port/out/                        # persistence ports
│   └── service/                         # use-case implementations
├── adapter/
│   ├── in/web/                          # controllers, response DTOs, springdoc annotations
│   └── out/persistence/                 # JPA entities, Spring Data repos, mappers, port impls
└── config/                              # wiring for non-Spring domain objects
```

## Architecture rules (ArchUnit enforces them)

- Dependencies point inward: `adapter → application → domain`. The domain and legacy classes never import Spring,
  JPA, Jackson or springdoc.
- Controllers depend only on `port/in` interfaces. JPA entities never leave `adapter/out/persistence`.
- `withdrawals` is **not** part of `TimeDeposit`. The GET response is a read model that combines a deposit with its
  withdrawals.
- Interest uses a Strategy pattern: one `InterestPolicy` per plan type, resolved by plan type. The shared
  "no interest in the first 30 days" rule lives in one place. To add a plan you add a class; you never edit
  existing policies or the calculator.
- `TimeDepositCalculator` keeps a no-arg constructor that wires the default policies, so existing callers work
  unchanged.
- The schema is defined only in Flyway migrations (`src/main/resources/db/migration`). Hibernate `ddl-auto` stays
  at `validate`. Write migrations with the `flyway-migration` skill. Committed migrations are never edited, so
  schema changes are a new `V<n>__*.sql`.

## Testing

Write tests with the `writing-tests` skill. It covers which test type to pick, the conventions, and how to verify.

- Characterization tests for `updateBalance` (each plan, the 30/31, 45/46 and 365/366 day boundaries, rounding,
  unknown plan) are the safety net for every refactor.
- Write the test first for new code. Unit tests (domain and application) run without a Spring context, with ports
  mocked.
- Integration tests run against real PostgreSQL via Testcontainers: annotate them with
  `@Import(PostgresTestcontainersConfiguration.class)`. Don't create containers per test class. Keep its image in
  sync with `compose.yaml`.
- `OpenApiContractTest` compares the live `/v3/api-docs.yaml` with the committed `openapi.yaml`. After an
  intentional API change, regenerate it and commit it in the same commit.
- Coverage (JaCoCo) is measured on every `verify`. A gate requires **100% line and branch coverage of
  `TimeDepositCalculator`**, so the legacy logic stays fully covered through the refactor. Never lower or remove the
  gate. When the logic moves into interest policies, extend the gate to them.
- `./mvnw clean verify` must pass before every commit.
