# 0008. Strategy pattern for interest calculation

## Status
Accepted (2026-11-25)

## Context
`TimeDepositCalculator.updateBalance` calculated interest in one nested `if/else` on the plan type. The README
requires:
- "Ensure the design is extensible to accommodate future complexities in interest calculations" and SOLID principles
- no breaking changes to `TimeDeposit`, an unchanged `updateBalance` signature, and unchanged behaviour
- "No interest is applied for the first 30 days for any **existing** plans"

72 characterization tests (100% line and branch coverage, mutation-checked in both directions of every boundary) pin
the original behaviour to the cent and must pass unedited.

## Decision
Strategy pattern, in `domain/interest/`:

- `InterestPolicy`: `planType()` plus `monthlyInterest(TimeDeposit)`. It returns the **raw, unrounded** monthly
  interest (`0.0` when not eligible) and never modifies the deposit.
- One class per plan: `BasicInterestPolicy` (1%), `StudentInterestPolicy` (3%, up to and including day 365) and
  `PremiumInterestPolicy` (5%, from day 46).
- `GracePeriod`: a **decorator** for the 30-day rule. `InterestPolicies.defaults()` wraps each existing plan in it,
  so the rule is written once. A future plan can opt out, which matches the README's "existing plans".
- `InterestPolicies`: a registry with exact, case-sensitive plan-type lookup. An unknown plan type gets `0.0`, and
  duplicate plan types are rejected when the registry is built.
- `TimeDepositCalculator` keeps its package, its `updateBalance(List<TimeDeposit>)` signature and the no-arg
  constructor (which uses `InterestPolicies.defaults()`). It gains a constructor taking `InterestPolicies`. It only
  rounds and applies the interest.

Two details were preserved deliberately, because changing them alters balances by a cent:
- **Operation order:** each policy computes `balance * ANNUAL_RATE / 12`, i.e. `(balance * rate) / 12`. Folding the
  rate (`balance * (rate / 12)`) gives different doubles (e.g. student 1.02: `0.0025499999999999997` vs `0.00255`).
- **Rounding** stays in the calculator, unchanged: `new BigDecimal(interest).setScale(2, HALF_UP)` on the exact
  binary value, not `BigDecimal.valueOf`.

*Assumption (invalid input, out of scope per README):* a `null` plan type used to throw a `NullPointerException`
after day 30. Now it has no policy and gets no interest, like any unknown plan type. That case was intentionally
never pinned.

## Consequences
- **SRP:** each policy holds one plan's rule, `GracePeriod` one cross-plan rule, the registry only resolves, and the
  calculator only rounds and applies.
- **OCP:** a new plan is a new `InterestPolicy` plus one line in `InterestPolicies.defaults()`. No existing policy
  or the calculator changes. `InterestPoliciesTest` shows an extra plan working through the unchanged calculator.
- **LSP/ISP/DIP:** all policies, decorated or not, honour the same small contract. The calculator depends on the
  abstraction, supplied through its constructor.
- Proof of unchanged behaviour: the 72 frozen characterization tests pass unedited, and 13 injected mutations in the
  new classes are caught by those frozen tests alone. The JaCoCo gate now also requires 100% line and branch coverage
  of `domain.interest`, and ArchUnit keeps every `InterestPolicy` in `domain.interest`.
- `double` arithmetic is kept on purpose for behavioural compatibility. Money is converted to exact decimals only at
  the storage and JSON boundaries (ADR 0004, ADR 0007).
- There are more classes than the original single method. That's accepted as the price of extensibility.
- Rejected: a single parameterized "rate policy" class (blurs the per-plan rules and conditions), the grace period
  as a global rule in the registry (it would force the rule on every future plan, contrary to "existing plans"), and
  plan-type `if/else` or `switch` in the calculator (every new plan would edit the calculator, violating OCP).
