# 0006. Code-first OpenAPI with a committed, test-verified contract

## Status
Accepted (2026-11-25)

## Context
README preferred stack: "Use an OpenAPI Swagger contract", and the submission must "provide clear instructions on
how to trigger the endpoints using the Swagger contract". The options were API-first (write the YAML, generate the
controllers) or code-first (annotate the controllers, generate the spec). A generated spec only exists while the app
runs, but reviewers should be able to read the contract without starting anything.

## Decision
- **Code-first** with springdoc-openapi 3.2.1 (built against Spring Boot 4.2.0). Controllers and DTOs carry OpenAPI
  annotations: `@Operation`, `@ApiResponse`, `@Schema` with descriptions, examples and required fields.
  springdoc serves the spec at `/v3/api-docs` (`.yaml` variant too) and Swagger UI at `/swagger-ui.html`.
- The generated spec is **committed** as `java/openapi.yaml`. `OpenApiContractTest` fetches `/v3/api-docs.yaml` and
  fails if it differs from the committed file. `./mvnw verify -Dopenapi.update=true` regenerates it.
- The spec is made deterministic so the comparison is reliable:
  - `springdoc.writer-with-order-by-keys=true`
  - a **relative** server URL (`/`) in `OpenApiConfiguration`, so the spec doesn't depend on the port or host
- The same test asserts the spec has **exactly** the two README paths and methods.
- API metadata (title, version, description) lives in `adapter/in/web/OpenApiConfiguration`, because OpenAPI is a
  web-adapter concern.

## Consequences
- One source of truth (the code), with no generator templates or generated sources to maintain.
- The committed contract can't drift silently. A controller change without a regenerated `openapi.yaml` fails the
  build (and `/commit-check` rule 8). API changes show up as a reviewable YAML diff.
- Swagger UI and `/v3/api-docs` are documentation endpoints, not business endpoints, so they don't count against the
  "exactly two endpoints" rule.
- The contract test needs a full application context (Testcontainers PostgreSQL), so it runs with the Docker-backed
  tests.
- Rejected: API-first with openapi-generator (generated controller interfaces and DTOs to maintain, more build
  complexity for two endpoints), and an uncommitted runtime-only spec (reviewers would have to run the app to see
  the contract).
