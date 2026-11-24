# 0001. Java 21 + Spring Boot, solution lives in `java/`

## Status
Accepted (2026-11-24)

## Context
The kata ships starters in C#, Java, Kotlin, Python and TypeScript, and we need exactly one implementation. The
README's preferred stack ("OpenAPI Swagger contract", "Hexagonal Architecture", "testcontainers") is well
supported on the JVM. The original `java/` project was a plain Maven/JUnit project on Java 17 with no web or
persistence layer, and Maven isn't installed on the development machine.

## Decision
- Implement the solution in the existing `java/` project. The other language folders stay untouched as the
  original kata reference.
- Upgrade to **Java 21** (LTS) and **Spring Boot 4.2** (`spring-boot-starter-parent`), starting with Web MVC only.
  Persistence, OpenAPI and Testcontainers dependencies arrive with the changes that use them.
- Use the **Maven wrapper** (`./mvnw`, Maven 3.10.1, only-script distribution), so a JDK is the only prerequisite.
- `TimeDeposit` and `TimeDepositCalculator` stay in `org.ikigaidigital`. `TimeDepositApplication` lives in the
  same root package, so component scanning covers all subpackages.

## Consequences
- Mature support for Spring Data JPA, Flyway, springdoc-openapi, Testcontainers (`@ServiceConnection`) and
  ArchUnit.
- The legacy classes are reused in place, so "no breaking changes" is checkable with `git diff`.
- The build is reproducible on any machine with JDK 21, with no global Maven needed.
- The repo keeps unused starters in other languages. That's accepted to preserve the original kata layout, and
  `CLAUDE.md` says clearly that only `java/` holds the solution.
- Rejected: Kotlin (fewer reviewers are familiar with it), C#/TypeScript (the README's preferred stack is weaker
  there for Testcontainers and hexagonal tooling), and a fresh project (it would lose the original classes' git
  history).
