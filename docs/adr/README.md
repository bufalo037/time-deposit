# Architecture Decision Records

Short records of significant decisions: context, decision, consequences. Format based on
[Michael Nygard's template](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).
When a decision changes, add a new ADR that supersedes the old one. Don't rewrite history.
Each ADR is committed together with the change that implements it, and a row is added here in the same commit.

| # | Decision | Status |
|---|---|---|
| [0001](0001-java-spring-boot.md) | Java 21 + Spring Boot, solution lives in `java/` | Accepted |

## Template

```markdown
# NNNN. Title

## Status
Proposed | Accepted | Superseded by NNNN (YYYY-MM-DD)

## Context
What forces are at play? What does the README require?

## Decision
What we do.

## Consequences
What gets easier or harder. The trade-offs we accept.
```
