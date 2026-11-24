---
name: flyway-migration
description: Write a Flyway database migration for the java/ module (PostgreSQL). Use it whenever a change creates or alters tables, columns, constraints, indexes or seed data, or when JPA entities need schema they don't have yet. Don't use it for changes that touch no schema, and never use it to edit an existing migration.
---

# Writing a Flyway migration

Flyway owns the schema (ADR 0002). Hibernate runs with `ddl-auto=validate`, so entities and migrations must match
exactly or the app won't start. Migrations are **append-only history**: once committed, a migration is never
edited, renamed or deleted.

## 1. Gather context

1. Read the schema requirements in `README.md` ("Database Setup") and any ADRs in `docs/adr/` about the schema
   (naming, types, keys). Follow recorded decisions exactly.
2. List existing migrations: `ls java/src/main/resources/db/migration`. Read them to learn the current schema.
3. Read the JPA entities in `adapter/out/persistence` that the migration has to serve.
4. **Open design questions** (e.g. naming convention, decimal precision, identity strategy, seed data contents)
   that no ADR answers: present 2–3 options with a recommendation and **ask the user**. If the answer shapes the
   schema design, record it with the `adr` skill in the same commit.

## 2. Create the file

- Location: `java/src/main/resources/db/migration/`
- Name: `V<n>__<snake_case_description>.sql`. `<n>` is the next integer after the highest existing version
  (`V1`, `V2`, …), followed by **two** underscores. Examples: `V1__create_time_deposits_and_withdrawals.sql`,
  `V2__seed_time_deposits.sql`.
- One purpose per migration. Keep schema and seed data in separate files.
- Plain PostgreSQL SQL. PostgreSQL DDL is transactional, so a failing migration rolls back fully. Don't wrap it in
  `BEGIN`/`COMMIT` yourself.

## 3. SQL rules

- **Explicit constraints, with names**, so errors and later changes are readable:
  - `CONSTRAINT pk_<table> PRIMARY KEY (id)`
  - `CONSTRAINT fk_<table>_<ref> FOREIGN KEY (<col>) REFERENCES <ref_table> (id)`
  - `NOT NULL` on every column the README marks as required.
- **Index every foreign key column** (PostgreSQL doesn't do it automatically):
  `CREATE INDEX idx_<table>_<col> ON <table> (<col>);`
- **Types:** map README types deliberately: Integer → `integer`, String → `varchar(n)` or `text`, Decimal →
  `numeric(p,s)` (exact, never `float`/`double precision` for money), Date → `date`. State every chosen size or
  precision in an ADR or a SQL comment.
- **No defensive hacks:** no `IF NOT EXISTS`, `DROP ... IF EXISTS` or `CREATE OR REPLACE` for tables. Flyway
  already guarantees each migration runs exactly once.
- Comment the *why* for any non-obvious choice or assumption (README: "justify them in code comments").
- Seed data: deterministic `INSERT`s with explicit ids. If ids come from an identity column, reset the sequence
  afterwards (`SELECT setval(...)`) so later inserts don't collide.

## 4. Changing an existing schema

- **Never edit** a migration that's already committed. Flyway's checksum validation would fail on every database
  that already ran it.
- Add a new `V<n+1>__...sql` with `ALTER TABLE ...`. Flyway Community has no undo, so a mistake is fixed with
  another forward migration.
- The only exception: a migration that exists **only in the working tree** (never committed) can still be edited.

## 5. Verify

1. Keep the entities in sync: column names, types, nullability and relationships must match, or
   `ddl-auto=validate` fails at startup.
2. Run `cd java && ./mvnw clean verify`. Every `@SpringBootTest` that imports
   `PostgresTestcontainersConfiguration` applies all migrations to a fresh PostgreSQL. Look for
   `Successfully applied N migrations` in the output. A Flyway or Hibernate validation error is a failing build.
3. For non-trivial schema, add or extend a persistence integration test that writes and reads through the adapter,
   proving the constraints and mappings work against real PostgreSQL.

## 6. Commit

- Commit the migration **together with** the code that needs it (entities, adapter, tests), plus any ADR, in one
  atomic commit (Conventional Commits, e.g. `feat: persist time deposits and withdrawals`).
- Run `/commit-check` before committing: rule 9 checks that no committed migration was edited.
- If the work was AI-assisted, add a work-log row to `docs/AI_USAGE.md`.
