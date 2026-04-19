# k6 — Two-Stage Performance Engineering

## The Problem

Performance testing as a late-stage activity means regressions are discovered after they are buried in 6 sprints of code. By then, root cause takes days to isolate. A 200ms latency regression introduced in a Tuesday morning commit shows up as a Friday afternoon incident. With component-level gates on every PR, the engineer who introduced the regression sees it within 60 seconds of opening the pull request.

---

## Stage 1 — Component Gates (Every PR)

One script per endpoint. Strict thresholds. Runs in parallel with REST Assured and Karate — it is an independent quality gate, not a sequential step.

| Script | Endpoint | Threshold |
|--------|----------|-----------|
| `component/patient-lookup.js` | `GET /users/{id}` | p95 < 500ms |
| `component/prescription-api.js` | `POST /posts` | p95 < 500ms |
| `component/cart-api.js` | `GET /posts?userId={id}` | p95 < 500ms |

Stage shape: 15s ramp to 5 VUs → 30s hold → 15s ramp down. Total runtime: 60 seconds per script.

---

## Stage 2 — System Load (Scheduled / Staging)

Full prescription checkout journey. Three endpoints in sequence with randomized think time (1–3 seconds) simulating realistic user pacing. Runs after Karate smoke on staging deploy.

**`system/prescription-checkout-load.js`** — sustained load at 20 VUs for 3 minutes. Validates the system holds p95 < 2000ms under realistic concurrent usage.

**`system/prescription-checkout-stress.js`** — ramps to 200 VUs over 10 minutes. Not a pass/fail gate — a break point discovery run. Results feed capacity planning and auto-scaling threshold decisions. Runs on a schedule (Monday 2 AM UTC), not on every deployment.

---

## Thresholds and Why

**p95 vs p99** — p95 is the operational SLA: 95% of users get a response under this time. p99 catches tail latency that affects 1 in 100 requests — important in a checkout flow where slow responses compound across service calls. Monitoring both catches different failure modes.

**Component threshold 500ms vs system threshold 2000ms** — A single endpoint call should complete in 500ms with no integration overhead. A full checkout journey crosses three services with think time; 2000ms is the realistic production SLA for the complete flow, not a relaxed standard.

**Error rate 1%** — Industry standard for production API error tolerance. Above 1%, the user impact is measurable. The threshold is the same for component and system tests because an elevated error rate at any level indicates a real problem.

---

## Running Locally

```bash
# Component test — single endpoint
k6 run --env API_BASE_URL=https://jsonplaceholder.typicode.com \
  k6/component/patient-lookup.js

# System load test — full checkout journey
k6 run --env API_BASE_URL=https://jsonplaceholder.typicode.com \
  k6/system/prescription-checkout-load.js

# Stress test — break point discovery
k6 run --env API_BASE_URL=https://jsonplaceholder.typicode.com \
  k6/system/prescription-checkout-stress.js
```

---

## Azure Load Testing — Production Path

Azure Load Testing executes k6 scripts as managed, distributed load on Azure Kubernetes infrastructure. No k6 infrastructure to maintain. Load generation scales horizontally across regions. Native GitHub Actions integration via `azure/load-testing` action — the same scripts in this repo run as Azure-managed jobs by changing one workflow step. Results publish to Azure portal with built-in comparison across runs. This is the production execution path; the local k6 scripts are the source of truth regardless of where they run.

Not implemented in this repo. The scripts are ready.

---

## Pipeline Integration

| Stage | Trigger | Scripts | Gate |
|-------|---------|---------|------|
| Component | Every PR | `component/*.js` | All three must pass; blocks Pact consumer |
| System load | Staging deploy | `system/prescription-checkout-load.js` | Runs after Karate smoke |
| Stress | Monday 2 AM UTC | `system/prescription-checkout-stress.js` | Scheduled; results inform capacity planning |
