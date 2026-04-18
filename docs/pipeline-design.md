# Pipeline Design

## The Principle

A test in the wrong gate is quality theater. Catching a contract violation post-deploy costs a rollback and an incident. Catching it on PR costs a conversation. Every gate in this pipeline has an explicit rationale for when it runs — not just what it runs.

---

## On Pull Request

**Workflow:** `pr-quality-gate.yml` | **Trigger:** PR → `main` or `develop`

| Job | What it validates | Why here |
|-----|-------------------|----------|
| REST Assured | HTTP behavior, schema, SLA for the changed service | Fastest signal on functional regressions; scoped to the module under change |
| Karate @smoke | Critical paths across service boundaries | Readable by the PR reviewer; catches cross-service breakage |
| Pact consumer | Generates the contract artifact from the changed consumer code | Contract must come from known-good code — functional tests gate this |
| Pact provider | Verifies the provider still satisfies all consumer contracts | A provider change that breaks a downstream consumer is blocked here, not in production |

Jobs 1 and 2 run in parallel. Job 3 waits on both. Job 4 waits on Job 3. Wall time under 5 minutes.

**Merge block is absolute.** Any failure blocks merge. No bypass without QE Director approval.

---

## On Merge

Nothing additional runs. The PR gate is the quality gate. Triggering the same tests again on merge validates the pipeline, not the code — and adds latency to a signal that already exists.

---

## On Staging Deploy

**Workflow:** `staging-smoke.yml` | **Trigger:** `workflow_dispatch` (initiated by deployment pipeline post-rollout)

Runs Karate `@smoke` scenarios only against the live staging endpoint.

This gate validates the **deployment**, not the code: DNS resolution, ingress routing, service mesh config, secrets injection. The code was already validated before the artifact was promoted. REST Assured and Pact do not re-run here — they would prove nothing new.

If smoke fails, staging is unhealthy. Stop deployments until resolved.

---

## The Dependency Chain

```
rest-assured-functional ──┐
                           ├──→ pact-consumer ──→ pact-provider
karate-bdd ────────────────┘
```

Pact consumer runs after functional tests because a contract generated from broken code is wrong by definition — it encodes the bug as a requirement and trains the provider to satisfy incorrect expectations. The sequence is not a convenience; it is a correctness constraint.

---

## Coverage Intelligence — Next Layer

SeaLights (or equivalent) sits above this platform as the coverage analytics layer. It answers the question this pipeline cannot: **which lines of application code are exercised by which tests, and which are exercised by none.**

Once integrated, SeaLights enables test impact analysis — running only the tests that cover changed code paths rather than the full suite on every PR. At platform scale (12+ services, 40+ engineers), that is the difference between a 5-minute gate and a 20-minute gate.

Not implemented in this reference. Noted here because the architecture must accommodate it: test results from all three layers need to be tagged with build metadata and pushed to the SeaLights agent as a post-step in each job.
