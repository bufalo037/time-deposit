# 0007. API endpoints: paths, methods and response format

## Status
Accepted (2026-11-25)

## Context
README: "Create a RESTful API endpoint to update the balances of all time deposits" and "to retrieve all time
deposits" with the schema `id, planType, balance, days, withdrawals`, and "exactly two API endpoints". It doesn't
name paths or methods, or define the shape of `withdrawals` or the money format.

## Decision

| Purpose | Method and path | Success |
|---|---|---|
| Retrieve all time deposits | `GET /time-deposits` | `200`, `application/json` array ordered by id |
| Update all balances | `POST /time-deposits/balances/update` | `204 No Content` |

- **POST, not PUT:** each call applies one more month of interest, so the operation is **not idempotent**, and PUT
  implies idempotency. Other methods on the update path return `405`.
- **204 without a body:** the client reads the new state with the GET endpoint. Returning the list would duplicate
  that endpoint's responsibility.
- **Response schema** (`TimeDepositResponse`): exactly `id`, `planType`, `balance`, `days`, `withdrawals`.
  Each withdrawal is `{ id, amount, date }`.
  - *Assumption:* the README names `withdrawals` but not its shape. The full records are the most useful and least
    lossy choice. `timeDepositId` is left out because the withdrawal is nested under its deposit.
- **Money** (`balance`, `amount`): a JSON **number with 2 decimals** (e.g. `10008.33`), serialized from
  `BigDecimal` at scale 2 (`BigDecimal.valueOf(double)`, `HALF_UP`), so double artifacts never reach clients.
  This matches the stored `numeric(19,2)` (ADR 0004).
- **Dates:** ISO-8601 strings (`2026-09-15`).
- *Assumption:* the update doesn't advance `days`. `updateBalance` never changes days, and the README gives no rule
  for it, so repeated calls compound interest on the same `days`. That's documented in the operation description.
- No other business endpoints: no single-resource GET, no create/delete. Invalid input and exception handling are out
  of scope (README).

## Consequences
- The API matches the README exactly. `OpenApiContractTest` checks the two paths and methods, and
  `TimeDepositControllerTest` checks the exact JSON (strict comparison: no extra fields).
- Clients must call GET after POST to see results. That's one extra round trip, accepted for single-responsibility
  endpoints.
- Repeated POSTs keep adding interest. That's intended (one call = one month) and documented in Swagger.
- Rejected: `PUT /time-deposits/balances` (wrongly implies idempotency), a `200` with the updated list (duplicates
  the GET), and money as JSON strings (less natural for this API and needs explaining).
