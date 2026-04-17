# Architecture Decision Record: API Quality Platform

**Status:** Accepted  
**Date:** 2024-Q1  
**Context:** Walgreens Digital QE Platform modernization

---

## Decision

Replace Postman/Newman as the CI regression layer with a three-module Maven platform: REST Assured (functional), Karate (BDD governance), Pact (contract).

## Alternatives Considered

| Option | Rejected Because |
|--------|-----------------|
| Postman at scale | No code review for test changes; secrets in exported JSON; no contract awareness |
| Cucumber + REST Assured only | Glue code overhead; no contract testing; doesn't solve governance |
| Karate only | No native Pact broker integration; Java interop for OAuth2/mTLS is fragile |
| Spring Cloud Contract | Provider-owned contracts invert the consumer-driven model; wrong for microservices mesh |

## Consequences

- **Positive:** Contract violations surface at PR time, not production. Test changes are PR-reviewable. Environment credentials stay in GitHub secrets.
- **Positive:** A single `mvn verify` runs all three layers. Dependency versions are enforced by the parent POM.
- **Negative:** Higher entry bar for non-Java QA contributors on REST Assured layer (mitigated by Karate for scenario authoring).
- **Negative:** Pact broker infrastructure required for full contract propagation across teams. JSONPlaceholder used as stand-in for local development.

## Service Boundary Map (Walgreens Digital — reference)

```
Cart Service (consumer)
  └─ depends on → Patient Service (provider)  ← Pact contract here
  └─ depends on → Prescription Service (provider)  ← Pact contract here

Prescription Service (consumer)
  └─ depends on → Patient Service (provider)  ← Pact contract here
  └─ depends on → Inventory Service (provider)

Notification Service (consumer)
  └─ depends on → Prescription Service (provider)
```

Each arrow is a contract. Each contract is a Pact consumer test on the consuming side and a provider verification on the providing side.
