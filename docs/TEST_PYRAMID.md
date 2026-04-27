# Test Pyramid — Complete Quality Engineering Strategy

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
| E2E Integration | Karate `@smoke` | Cross-service business flow failures | Staging deploy |
| Service Virtualization | Hoverfly / WireMock | Integration failures in isolation | PR + staging |
| Contract Testing | Pact | Interface contract violations | PR + deploy |
| Component Functional | REST Assured + Karate | Endpoint behavior regressions | Every PR |
| Performance Engineering | k6 | SLA violations at component and system level | PR + staging + scheduled |
| Unit Testing | Developer framework | Logic and calculation errors | Every commit |

---

## What This Repository Implements

Four of the six layers are implemented and running in the pipeline:

- **Component Functional** — REST Assured + Karate ✅
- **Contract Testing** — Pact consumer/provider ✅
- **Performance Engineering** — k6 two-stage ✅
- **E2E Integration** — Karate `@smoke` on staging ✅

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
