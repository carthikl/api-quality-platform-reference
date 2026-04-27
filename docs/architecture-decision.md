# Architecture Decision: API Quality Platform

**Status:** Accepted | **Context:** Walgreens Digital QE Platform — Java/Kubernetes microservices on Azure

---

## Context

Postman/Newman fails at three things that matter at platform scale:

**Governance.** When 40 engineers across 12 squads own collections in personal accounts, there is no reviewable artifact. Test changes ship without approval, environments drift silently, and there is no audit trail. This is a process problem Postman's architecture cannot solve.

**Secrets management.** Postman environments export credentials as plaintext JSON. In a HIPAA/PCI environment, credentials in committed files or personal accounts are a compliance violation, not a configuration issue.

**Contract visibility.** Newman validates responses against hand-maintained payloads. It has no mechanism to detect when a provider team changes a response schema and silently breaks every downstream consumer. In a microservices mesh this failure mode is invisible until production.

---

## Decision

Three-layer Maven multi-module platform, each layer solving exactly one of the above:

- **REST Assured** — deep payload validation, auth flows, and stateful sequences that require Java logic
- **Karate** — governed BDD scenarios reviewable by non-engineers; test changes are PR-gated artifacts, not personal account exports
- **Pact** — consumer-driven contracts that surface schema drift between services at PR time, not in production
- **k6** — two-stage performance engineering; performance caught late in the cycle
  - Governance note: k6 integrated via exec-maven-plugin to provide Maven execution parity alongside direct CLI execution. Teams standardizing on `mvn test` as single entry point can use: `mvn test -pl k6`

---

## Alternatives Considered

**Postman only** — governance and secrets problems are architectural; no tooling layer fixes them inside Postman's model.

**Playwright API only** — TypeScript-first, no Pact broker integration, requires a full Node.js toolchain alongside Java services. Adds runtime heterogeneity for no gain.

**Karate only** — no native Pact broker integration; OAuth2 PKCE and mTLS flows require Java interop workarounds that undermine Karate's readability advantage.

---

## Trade-offs

**REST Assured:** Non-Java QA engineers cannot own these tests without ramp-up. Karate handles scenario authoring; REST Assured handles auth complexity.

**Karate:** Parallel execution and environment switching are built in, but Karate's Java interop for advanced flows is fragile at the edges. Scope it to what it does well.

**Pact:** Requires a Pact Broker to propagate contracts across teams. Without the broker, contract coverage is limited to what lives in a single repo. That infrastructure investment is required before cross-team guarantees apply.

---

## What This Replaces

Postman's role narrows to **exploration and documentation** — the use cases it is genuinely good at. It is removed from CI entirely. Newman is removed from CI entirely. Manual environment JSON management is removed entirely.

---

## When to Evolve

**If the org moves to TypeScript microservices:** Evaluate Pact JS for consumer tests and Playwright for functional validation. The three-layer model holds; the implementation stack changes.

**If Java expertise gaps widen on QE teams:** Shift more coverage to Karate feature files. Reduce the REST Assured surface to auth flows and SLA validation only — the two cases where Java logic earns its cost.
