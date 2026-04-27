# API Quality Engineering Test Pyramid
## API and Service Layer Coverage — What This Pyramid Covers and What It Does Not

---

## Scope of This Pyramid

This pyramid covers the API and service layer of the quality engineering stack — the five layers implemented in this reference architecture. It is one slice of a complete quality engineering strategy, not the whole picture.

A complete QE strategy for a modern digital platform also requires the layers documented below under "What This Pyramid Does Not Cover". Each gap is named with the recommended tool and the reason it sits outside this reference implementation.

---

## The Principle

The testing pyramid is not a tool selection framework. It is a risk distribution model. Each layer catches a different category of defect at the lowest possible cost — and cost is measured in time, compute, and feedback delay, not just money. The higher the layer, the more expensive the defect it catches: a cross-service business flow failure found in staging costs an order of magnitude more to diagnose and remediate than a contract violation caught at PR merge. The lower the layer, the faster the signal returns to the developer. Designing a quality engineering strategy means deliberately placing each category of risk at the layer where it can be caught earliest and cheapest.

---

## The Complete Pyramid

```
                        ▲
                       /|\
                      / | \
                     /  |  \
                    / E2E/  \
                   / Integration\
                  /  (Karate @smoke)\
                 /___________________\
                /                     \
               /  Service Virtualization\
              /   (Hoverfly / WireMock)  \
             /____________________________\
            /                              \
           /      Contract Testing          \
          /         (Pact)                   \
         /______________________________________\
        /                                        \
       /       Component Functional Testing        \
      /        (REST Assured + Karate)              \
     /______________________________________________ \
    /                                                 \
   /          Performance Engineering                  \
  /               (k6)                                  \
 /______________________________________________________ \
/                                                         \
/                   Unit Testing                           \
/              (Developer Framework)                        \
/___________________________________________________________\
```

---

## What Each Layer Catches

| Layer | Tool | Defect Category | When It Runs |
|---|---|---|---|
| E2E Cross-Service Journey | Karate `@e2e` | Business flow failures across all three microservices | Merge to main + staging deploy |
| E2E Integration | Karate `@smoke` | Critical path failures on staging | Staging deploy |
| Service Virtualization | Hoverfly / WireMock | Integration failures in isolation | PR + staging |
| Contract Testing | Pact | Interface contract violations | PR + deploy |
| Component Functional | REST Assured + Karate | Endpoint behavior regressions | Every PR |
| Performance Engineering | k6 | SLA violations at component and system level | PR + staging + scheduled |
| Unit Testing | Developer framework | Logic and calculation errors | Every commit |

---

## What This Repository Implements

Five of the seven layers are implemented and running in the pipeline:

- **Component Functional** — REST Assured + Karate ✅
- **Contract Testing** — Pact consumer/provider ✅
- **Performance Engineering** — k6 two-stage ✅
- **E2E Integration** — Karate `@smoke` on staging ✅
- **E2E Cross-Service Journey** — Karate `@e2e` prescription checkout journey ✅

---

## E2E Cross-Service Journey — The Deliberate Decision

The E2E prescription checkout journey (`@e2e`) sits above the existing Karate `@smoke` layer. It tests a single business scenario — patient lookup through checkout confirmation — chaining three microservices in sequence: PatientService, PrescriptionService, and CartService. Each step extracts data from the previous service response and uses it as input to the next call.

**Why E2E runs on staging, not in the PR gate:**

The PR gate validates individual service behavior — does this endpoint return the right schema, the right status code, the right contract? That question can be answered in isolation, without other services being live. E2E asks a different question: does the integrated system — all three services, running together with their real databases and real network paths — execute the full business flow correctly? That question can only be answered against a real deployment.

Blocking a PR on an E2E journey test would couple every engineer's PR velocity to the health of the staging environment. A flaky deployment, a misconfigured secret, or a transient network partition in staging would block unrelated code changes. The cost of that coupling is disproportionate to the benefit, because contract testing already catches the interface violations that would cause E2E to fail.

**The deliberate placement:**

- PR gate: Pact catches interface contract violations before merge. If `PatientService` renames a field, the Pact verification fails on the PR — not in staging.
- Merge to main: E2E runs after pact-provider passes, confirming the integrated artifact is sound.
- Staging deploy: E2E `@smoke` runs after the Karate smoke gate passes, confirming the deployed system executes the full journey.

This is not a gap. It is a deliberate architectural decision: test the right thing at the right boundary.

---

## The Gap — Service Virtualization

Service virtualization sits between component testing and full integration testing. It allows services to be tested in combination without all dependencies being live — catching integration failures in isolation, before a full staging environment is required.

**Hoverfly** is lightweight, Go-based, and designed for microservices. It captures real traffic and replays it as stubs, making it CI/CD native with minimal configuration overhead. Ideal for Java/Kubernetes environments where teams want fast, low-friction dependency stubbing at the network layer.

**WireMock** is Java-native with a deep enterprise ecosystem. More configuration overhead than Hoverfly, but stronger IDE integration, broader community adoption, and more expressive stubbing capabilities for complex scenarios.

**Why not implemented here:** JSONPlaceholder does not require virtualization — it is already a stub service. In a production Walgreens implementation, Hoverfly would virtualize the `PatientService` dependency so `PrescriptionService` can be tested in isolation without `PatientService` being deployed. A developer changing `PrescriptionService` business logic would run against a Hoverfly stub of `PatientService` in the PR pipeline — catching integration failures before the branch ever reaches staging. That is the production pattern. It is not demonstrated in this reference architecture because the reference uses a public mock API that eliminates the need for it.

---

## Inter-Service Functional Testing

The question of how to test functional behavior between microservices beyond contract validation is answered by three layers working together, not one. Pact validates the interface agreement — field names, types, and response structure — ensuring that a change to `PatientService` that renames `email` to `emailAddress` is caught before it reaches any downstream consumer. Hoverfly or WireMock then validates functional behavior with stubbed dependencies: business logic, error handling, and edge cases that require a dependency to be present but not necessarily live. Finally, Karate `@smoke` validates the full end-to-end journey with real services on staging, confirming that the system behaves correctly as an integrated whole under realistic conditions. Each layer is necessary. None is sufficient alone.

---

## The Operating Principle

"The pyramid is not a QA artifact. It is an engineering discipline — owned by the squads, governed by the platform, measured by the outcomes."

---

## What This Pyramid Does Not Cover

| Gap | Recommended Tool | Why Not In This Repo |
|---|---|---|
| UI / Browser E2E | Playwright or Selenium | Requires a frontend application. This repo tests APIs only — no React/HTML layer exists to drive. Playwright is the recommended addition for any team with a customer-facing UI. |
| Accessibility (WCAG 2.1 AA) | axe-core (integrated with Playwright or Karate UI) | Accessibility testing requires DOM traversal. Cannot be performed at the API layer. Must run against a rendered UI. WCAG 2.1 AA compliance is a legal requirement for customer-facing digital products. |
| Security Scanning (SAST) | Snyk Code or SonarQube | Static analysis runs against source code, not test output. Belongs in the PR pipeline as a separate stage alongside quality gates — not inside the test pyramid. |
| API Security (DAST) | OWASP ZAP | Dynamic application security testing requires a running application with intentionally vulnerable configurations. Separate pipeline stage from functional quality gates. |
| Compliance / Regulatory | Custom — HIPAA, PCI-DSS, pharmacy board requirements | Compliance testing is organization and regulation specific. Cannot be generalized in a reference implementation. Requires mapping regulatory requirements to testable specifications — a governance design exercise, not a tool selection. |
| Performance at System Scale | Gatling or k6 at load balancer level | k6 in this repo tests component-level SLAs (per endpoint) and system load (full journey). Infrastructure-level performance testing — DNS resolution, CDN behavior, load balancer distribution — requires a different tool and a different environment. |
| Chaos Engineering | Gremlin or AWS Fault Injection Simulator | Chaos engineering deliberately degrades production-like infrastructure. Requires infrastructure access and game day planning. Not a pipeline gate — a scheduled exercise. |
| Contract Testing at Scale | Pactflow (SaaS) or self-hosted Pact Broker | This repo uses local pact files. Production implementations require a broker with can-i-deploy gates. See docs/PACT_BROKER_GUIDE.md. |

Naming these gaps explicitly is not a limitation — it is an architectural decision. A reference implementation that claims completeness without the infrastructure to support it is not honest. Each gap above has a documented path to resolution when the right environment and organizational context exist.
