# Build Story — How This Was Created

## Why This Exists

This repository was built as a reference architecture 
for a Director SDET interview conversation at Walgreens. 
The mandate: demonstrate enterprise QE platform thinking 
without presenting a slide deck. Show the work instead.

## The Approach

Architected and directed using Claude Code as the 
implementation engine — the same operating model 
a Director uses with a senior SDET team. Every 
architectural decision was made by the architect:
- Three-layer structure and why
- Client/test separation pattern
- Pact V3 over V4 for HTTP REST contracts
- Pipeline gate sequencing and dependency chain
- What Postman replaces and what it doesn't

Claude Code handled Java syntax, Maven configuration, 
and GitHub Actions YAML generation. The architect 
specified what needed to exist, reviewed every output, 
and rejected anything that didn't meet the standard.

## What Was Built

| Layer | Files | Tests | Time |
|---|---|---|---|
| REST Assured | 6 Java files | 7 tests | ~2 hours |
| Karate | 4 files | 6 scenarios | ~45 mins |
| Pact | 2 Java files | 2 verifications | ~45 mins |
| GitHub Actions | 2 YAML files | 4-job pipeline | ~30 mins |
| Documentation | 4 Markdown files | — | ~45 mins |

## Actual Build Time

Total elapsed: approximately 8 hours across two days.
A senior SDET building this manually without AI 
assistance: 3-5 days minimum.

## Cost

- Claude.ai Pro subscription: $20/month (flat rate)
- Claude Code API usage: approximately $3-5
- Total additional cost beyond subscription: under $5

## The AI-Native Productivity Model

This is not vibe coding. Every prompt was intentional.
Every output was reviewed. Every deviation from spec 
was evaluated — accepted when the reasoning was sound, 
rejected when it wasn't.

The five Claude Code architectural deviations that 
were accepted:
1. PrescriptionApiClient separated from PatientApiClient 
   — single responsibility principle
2. Thread-safe spec per call, not singleton 
   — TestNG parallel execution safety
3. PactSpecVersion.V3 over V4 
   — HTTP REST contracts, not plugin-oriented DSL
4. HttpTestTarget.fromUrl() over constructor 
   — correct factory method for Pact 4.6.7
5. logback-test.xml added 
   — suppresses HttpClient wire logging correctly

## What This Demonstrates

- Systems thinking: three problems, three solutions, 
  one pipeline
- Director operating model: specify, direct, review — 
  not implement
- AI fluency: purposeful use of agentic tooling 
  with architectural guardrails
- Quality at Speed: 80-second pipeline gate on 
  every pull request

## What Would Come Next

In a real implementation:
- k6 performance layer — response time gates 
  on every staging deployment
- SeaLights integration — real-time coverage 
  intelligence across the pipeline
- Pactflow — hosted contract broker replacing 
  local pact file exchange
- Azure Load Testing — distributed k6 execution 
  on Kubernetes infrastructure
