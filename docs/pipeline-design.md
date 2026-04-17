# Pipeline Design: What Runs When and Why

## Gate Philosophy

A test in the wrong gate is worse than no test. Running slow Pact verification as a post-deploy check means a contract violation discovers itself in production. Running lightweight smoke tests as a PR gate means engineers wait 20 minutes for results that should take 2.

Every test in this platform has an assigned gate with an explicit rationale.

---

## PR Gate (`pr-quality-gate.yml`)

**Trigger:** Any PR targeting `main` or `develop`  
**SLA:** Must complete in under 10 minutes

| Stage | Tool | Runtime | Rationale |
|-------|------|---------|-----------|
| 1 | REST Assured | ~2 min | Validates HTTP contracts of the specific service being changed |
| 2 | Karate smoke | ~3 min | Broad coverage across service boundaries, readable by non-Java reviewers |
| 3 | Pact consumer | ~1 min | Generates contract artifact for the PR's consumer changes |
| 4 | Pact provider | ~2 min | Verifies provider hasn't broken any registered consumer contracts |

All four jobs run in parallel (1, 2, 3 independent; 4 depends on 3). Total wall time under 5 minutes with parallel execution.

**Merge block:** Any single failure blocks merge. No exceptions, no bypass without QE Director approval.

---

## Staging Smoke (`staging-smoke.yml`)

**Trigger:** `repository_dispatch` event from the deployment pipeline after a successful Kubernetes rollout  
**SLA:** Must complete in under 5 minutes

| Stage | Tool | Rationale |
|-------|------|-----------|
| Karate @smoke | Karate | Validates the deployment itself (DNS, ingress, service mesh, secrets injection) — not the code |

**What does NOT run:**
- REST Assured: code was already validated on PR. Running it again proves nothing about the code.
- Pact: contracts were validated before the artifact was promoted. Re-running is redundant.

**On failure:** PagerDuty alert (critical severity). Automatic rollback is the deployment pipeline's responsibility, not this workflow's.

---

## Secrets Strategy

| Secret | Scope | Rotation Owner |
|--------|-------|---------------|
| `PACT_BROKER_URL` | GitHub Actions environment | QE Platform team |
| `PACT_BROKER_TOKEN` | GitHub Actions environment | QE Platform team |
| `PAGERDUTY_INTEGRATION_KEY` | GitHub Actions environment | On-call team |
| `API_BASE_URL` (staging) | GitHub Actions environment | DevOps |

No secrets appear in pom.xml, karate-config.js, or `.feature` files. All secrets are injected as `-D` system properties at runtime by the CI runner.

---

## Local Developer Workflow

```bash
# Full suite — mirrors what CI runs
mvn verify -Dapi.base.url=https://jsonplaceholder.typicode.com

# Single module, fast feedback
mvn verify -pl rest-assured -Dapi.base.url=https://jsonplaceholder.typicode.com

# Smoke only (Karate)
mvn verify -pl karate \
  -Dkarate.options="--tags @smoke" \
  -Dapi.base.url=https://jsonplaceholder.typicode.com

# Pact consumer only (generate pact JSON, no broker publish)
mvn verify -pl pact \
  -Dtest='**/*ConsumerTest' \
  -Dapi.base.url=https://jsonplaceholder.typicode.com
```
