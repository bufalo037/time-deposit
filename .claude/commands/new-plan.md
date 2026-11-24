---
description: Scaffold a new interest plan type (strategy + tests) without touching existing policies
argument-hint: <plan-name> <annual-rate> [rules...]
---

Add a new time-deposit interest plan: $ARGUMENTS

Follow `java/CLAUDE.md`. Steps:

1. Read `org.ikigaidigital.domain.interest` to learn the current `InterestPolicy` contract and how policies are
   registered.
2. Write the failing unit tests first for the new policy. Cover the rate, every day-based rule, the shared 30-day
   grace period, and rounding to 2 decimals with HALF_UP.
3. Add one new `InterestPolicy` implementation and register it. **Don't modify existing policies,
   `TimeDepositCalculator` or `TimeDeposit`.** If you can't add the plan without editing them, stop and explain
   why the design isn't open for extension.
4. Run `./mvnw clean verify` from `java/`. The existing characterization tests must still pass untouched.
5. Justify any assumption about the rules in a code comment.
6. Propose one atomic commit message (`feat: add <plan> interest plan`). Don't commit unless asked.
