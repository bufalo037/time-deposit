---
name: writing-tests
description: Write or extend tests for the java/ module. Use it whenever adding or changing production code (test-first), pinning legacy behaviour before a refactor (characterization tests), or adding persistence, web, architecture or contract tests. It covers which test type to pick, the conventions, and how to verify. Never use it to weaken, delete or "fix" an existing characterization test.
---

# Writing tests

Tests are the safety net for the README's hardest constraint: `updateBalance` must behave **exactly** as before.
Pick the cheapest test type that proves the behaviour, write it first, and run the full build.

## 1. Pick the test type

| What you're testing | Test type | Spring context? | Docker? |
|---|---|---|---|
| Legacy `TimeDepositCalculator` behaviour before/while refactoring | **Characterization** (plain JUnit) | No | No |
| Domain logic (interest policies, models) | **Unit** (plain JUnit) | No | No |
| Application services (use cases) | **Unit** with Mockito mocks for `port/out` | No | No |
| Persistence adapter (entities, repositories, mappers, Flyway schema) | **Integration**: `@DataJpaTest` + Testcontainers | Slice | Yes |
| Web adapter (controllers, JSON shape, status codes) | **Web slice**: `@WebMvcTest` + `MockMvc`, use cases mocked with `@MockitoBean` | Slice | No |
| Both endpoints end to end, real DB | **End-to-end**: `@SpringBootTest` + Testcontainers + `MockMvc`/`RestClient` | Full | Yes |
| Hexagonal dependency rules | **ArchUnit** | No | No |
| Committed `openapi.yaml` matches the live spec | **Contract**: `OpenApiContractTest` | Full | Yes |

Prefer the lowest row that proves the behaviour. Most tests are unit tests. A few integration and end-to-end tests
prove the wiring.

## 2. Conventions

- **Location and naming:** mirror the production package under `src/test/java`. The class is `<ClassUnderTest>Test`.
  Method names describe behaviour: `appliesNoInterestDuringFirst30Days()`, not `test1()`.
- **Structure:** Arrange / Act / Assert, one behaviour per test, no logic (`if`/loops) in test bodies.
- **Assertions:** AssertJ only (`assertThat`). Assert on concrete values, never `isNotNull()` alone, and never a
  placeholder like `assertThat(1).isEqualTo(1)`.
- **Tables of cases:** use `@ParameterizedTest` with `@CsvSource` for boundaries and plan/day/balance combinations,
  with a readable `name = "..."`.
- **Deterministic:** fixed inputs, no randomness, no `Thread.sleep`, no dependence on the current date or test
  order. Build test data with small factory methods or builders local to the test.
- **Money:** assert exact expected balances. The legacy code rounds the monthly interest to 2 decimals
  (`HALF_UP`) and adds it to a `double`, so compare with `isEqualTo(expected)` using the observed value, not a
  tolerance that could hide a cent difference.
- **Spring Boot 4 imports:** test-slice annotations moved to new modules/packages in Boot 4 (e.g.
  `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, and `@MockitoBean` replacing `@MockBean`). Let
  the compiler confirm imports. Don't copy Boot 2/3 package names from memory.

## 3. Characterization tests (legacy calculator)

Purpose: pin **current** behaviour of `TimeDepositCalculator.updateBalance` so any refactor that changes a single
cent fails. Class: `TimeDepositCalculatorCharacterizationTest`.

1. Cover every branch and boundary: each plan (`basic`, `student`, `premium`) and an unknown plan, with days
   `0, 30, 31, 45, 46, 365, 366` and a large value. Include balances that exercise rounding (e.g. values whose
   monthly interest ends in .xx5), a zero balance, and a list with several deposits processed in one call.
2. Get the expected values by **running the current code**, then sanity-check them against the README rules. If
   the code and the README disagree, **pin the code's behaviour**, add a comment, and tell the user. The README
   says the existing `updateBalance` is correct.
3. Use the public API only: construct `TimeDeposit`, call `updateBalance(List)`, assert `getBalance()`.
4. Once committed, these tests are frozen. A refactor that breaks one is wrong. Never edit expected values to make
   a refactor pass.

## 4. Integration tests (Docker)

- Always `@Import(PostgresTestcontainersConfiguration.class)`. Never start containers per test class or hard-code
  connection settings. The shared config and Spring's context cache reuse one container.
- `@DataJpaTest` replaces the datasource with an embedded one by default. Add
  `@AutoConfigureTestDatabase(replace = Replace.NONE)` so it uses the Testcontainers PostgreSQL.
- Flyway runs all migrations on the fresh database, so the test also proves the schema. Insert test data through
  the adapter or repository under test, or with `@Sql` scripts in `src/test/resources`.
- Keep tests independent: rely on `@DataJpaTest`'s per-test rollback, or clean up explicitly in end-to-end tests.

## 5. Test-first workflow for new code

1. Write the failing test for the next small behaviour. Run it and see it fail for the right reason.
2. Write the minimum production code to pass.
3. Refactor with the whole suite green.
4. Repeat. Don't add production code that no test requires.

## 6. Verify

- Single class while iterating: `cd java && ./mvnw test -Dtest=ClassName` (pure unit tests need no Docker).
- Before finishing: `cd java && ./mvnw clean verify`. Report the test counts. All tests run here, including the
  Docker-backed ones.
- Check coverage in `target/site/jacoco/jacoco.csv` (or `index.html`) for the classes you touched, and report line
  and branch coverage. Uncovered branches in logic you changed mean missing tests. The JaCoCo gate fails the build
  if `TimeDepositCalculator` drops below 100% line or branch coverage.
- A test that passes on the first run, before the production code exists, proves nothing. Make sure it can fail
  (e.g. temporarily break the expectation) when the reason it passes isn't obvious.

## 7. Commit

- Tests go in the **same commit** as the production code they cover. Characterization tests are their own commit
  (`test: add characterization tests for updateBalance`), **before** any refactor commit.
- Run `/commit-check` before committing. It checks that characterization tests weren't weakened.
- If the work was AI-assisted, add a work-log row to `docs/AI_USAGE.md`.
