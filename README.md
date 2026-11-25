# Time Deposit Refactoring Kata - Take-Home Assignment

## Solution: run it and trigger the endpoints

The solution lives in [`java/`](java/) (Java 21, Spring Boot 4.2, PostgreSQL). The other language folders are the
untouched kata starters. The original assignment follows [below](#xa-bank-time-deposit).

### Prerequisites
- JDK 21
- Docker (running). The app starts its own PostgreSQL. Maven isn't needed: the project uses the Maven wrapper.

### Start the application
```bash
cd java
./mvnw spring-boot:run                      # http://localhost:8080
SERVER_PORT=18080 ./mvnw spring-boot:run    # alternative if port 8080 is taken
```
On startup, the database (`java/compose.yaml`) starts automatically, the schema is migrated (Flyway), and
**6 sample time deposits** are seeded, one per plan before and after its interest starts.

### Trigger the endpoints with the Swagger contract
1. Open **Swagger UI**: <http://localhost:8080/swagger-ui.html>
   (the contract is also committed as [`java/openapi.yaml`](java/openapi.yaml) and served at `/v3/api-docs.yaml`).
2. Expand **`GET /time-deposits`** → **Try it out** → **Execute**. You get all deposits with the README schema
   (`id`, `planType`, `balance`, `days`, `withdrawals`).
3. Expand **`POST /time-deposits/balances/update`** → **Try it out** → **Execute**. You get **`204 No Content`**.
4. Run **`GET /time-deposits`** again. One month of interest has been applied:

| id | plan | days | before | after one update | why |
|---|---|---|---|---|---|
| 1 | basic | 20 | 10000.00 | 10000.00 | no interest for the first 30 days |
| 2 | basic | 120 | 10000.00 | **10008.33** | 1% a year, applied monthly |
| 3 | student | 200 | 5000.00 | **5012.50** | 3% a year, applied monthly |
| 4 | student | 400 | 5000.00 | 5000.00 | student: no interest after 1 year |
| 5 | premium | 40 | 20000.00 | 20000.00 | premium: interest starts after 45 days |
| 6 | premium | 90 | 20000.00 | **20083.33** | 5% a year, applied monthly |

The same with curl:
```bash
curl http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/balances/update
```

> **Note:** each `POST` applies **another** month of interest (not idempotent). Data survives app restarts. To
> reset to the seed, stop the app and run `docker compose down` in `java/`.

### Run the tests
```bash
cd java
./mvnw clean verify     # unit, Testcontainers integration, end-to-end, OpenAPI contract and ArchUnit tests
```
The build also enforces a coverage gate (100% line and branch coverage of the interest logic). The report is at
`java/target/site/jacoco/index.html`.

### Design and AI-assisted development
- Architecture decisions: [`docs/adr/`](docs/adr/) (hexagonal architecture, schema and money, code-first OpenAPI,
  endpoints, …)
- AI tooling, setup, an AI-assisted work log and summary: [`docs/AI_USAGE.md`](docs/AI_USAGE.md)
- Architecture review with known limitations and follow-ups: [`docs/ARCHITECTURE_REVIEW.md`](docs/ARCHITECTURE_REVIEW.md)
- Agent rules: [`CLAUDE.md`](CLAUDE.md), [`java/CLAUDE.md`](java/CLAUDE.md)

---

## XA Bank Time Deposit

### Context
A junior developer implemented domain logic for a time deposit system but did not complete the API functionality. Your task is to refactor the existing codebase to implement all required functionalities based on the provided business requirements, ensuring no breaking changes occur.

### Requirements

1. **API Endpoints**:
    - Create a RESTful API endpoint to update the balances of all time deposits in the database.
    - Create a RESTful API endpoint to retrieve all time deposits.
        - The GET endpoint should return a list of all time deposits with the following schema:
            - `id`
            - `planType`
            - `balance`
            - `days`
            - `withdrawals`

2. **Database Setup**:
    - Store all time deposit plans in a database.
    - Define the following tables:
        - `timeDeposits`:
            - `id`: Integer (primary key)
            - `planType`: String (required)
            - `days`: Integer (required)
            - `balance`: Decimal (required)
        - `withdrawals`:
            - `id`: Integer (primary key)
            - `timeDepositId`: Integer (foreign key, required)
            - `amount`: Decimal (required)
            - `date`: Date (required)

3. **Interest Calculation**:
    - Implement logic to calculate monthly interest based on the plan type:
        - **Basic Plan**: 1% interest
        - **Student Plan**: 3% interest (no interest after 1 year)
        - **Premium Plan**: 5% interest (interest starts after 45 days)
    - No interest is applied for the first 30 days for any existing plans.

4. **Refactoring Constraints**:
    - Do not introduce breaking changes to the shared `TimeDeposit` class or modify the `updateBalance` method signature.
    - Ensure the design is extensible to accommodate future complexities in interest calculations.

5. **Code Quality**:
    - Adhere to SOLID principles, design patterns, and clean code practices where applicable.

6. **AI-Assisted Development**:
    - Set up an AI harness or agent workflow and use it throughout the development for this take-home exercise.
    - Briefly document the tools and setup used (e.g., LLMs, coding assistants, agentic frameworks, configuration).
    - Ensure your AI setup is practical and reproducible.
    - Include any custom rules, system prompts, or agent configurations used.
    - Include a brief summary of which parts of the solution were AI-assisted and why.

### Important Guidelines
- The existing `TimeDepositCalculator.updateBalance` method is functioning correctly. Ensure its behavior remains unchanged after refactoring.
- The final solution must include **exactly two API endpoints**. Do not develop additional endpoints.
- **Do not** create a pull request or a new branch in the ikigai-digital repository. Instead, fork the repository into your own GitHub repository and develop the solution there.
- Handling invalid input or exceptions is not required.
- Use any tools, frameworks, or libraries you find suitable.
- In case of ambiguity, make logical assumptions and justify them in code comments.

### Preferred Stack
- Use an OpenAPI Swagger contract.
- Embrace Hexagonal Architecture.
- Follow atomic commit practices.
- Utilize testcontainers.
- Leverage AI-assisted development tools for code generation, testing, and refactoring.

### Submission Instructions
- Provide clear instructions on how to trigger the endpoints using the Swagger contract.
- Email the link to your public GitHub repository.