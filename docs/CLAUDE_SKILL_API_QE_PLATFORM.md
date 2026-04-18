# CLAUDE_SKILL_API_QE_PLATFORM.md

## Skill: API Quality Engineering Platform

---

### Purpose

This skill builds a production-quality, three-layer API testing reference architecture on a Java/Maven multi-module project. Use it when replacing Postman/Newman in a CI pipeline, demonstrating enterprise QE platform thinking, or establishing a governed, code-reviewed testing foundation for a Java microservices org. The output is a fully working Maven project with REST Assured (functional validation), Karate DSL (BDD governance), and Pact (consumer-driven contract testing), wired together in a GitHub Actions pipeline with a 4-job dependency chain. Every layer is independently buildable and independently runnable against any REST API.

---

### Prerequisites

- Java 17+
- Maven 3.9+
- GitHub repository created
- Public mock API available (JSONPlaceholder default: `https://jsonplaceholder.typicode.com`)

---

### Variables to Customize

Replace these values throughout all prompts before using them.

| Variable | Default | Description |
|---|---|---|
| `PROJECT_NAME` | `api-quality-platform-reference` | GitHub repo name and root Maven artifactId |
| `PACKAGE_BASE` | `com.wag.qe` | Java package root for all source files |
| `CONSUMER_NAME` | `PrescriptionService` | Pact consumer service name (the service making calls) |
| `PROVIDER_NAME` | `PatientService` | Pact provider service name (the service receiving calls) |
| `TARGET_API` | `https://jsonplaceholder.typicode.com` | Mock API base URL used in CI and local runs |
| `DOMAIN` | `prescription/patient` | Business domain for test naming, feature file directories, and scenario language |

---

### Prompt 1 — Project Scaffolding

```
I'm building a Java API testing reference architecture to demonstrate enterprise QE 
platform thinking. I need you to scaffold the complete project structure.

Project name: PROJECT_NAME
Java package root: PACKAGE_BASE
Target mock API: TARGET_API

Build a Maven multi-module project with this exact structure:

PROJECT_NAME/
├── pom.xml                          (parent POM)
├── README.md                        (8-section architect-to-VP narrative)
├── rest-assured/
│   └── pom.xml
├── karate/
│   └── pom.xml
├── pact/
│   └── pom.xml
└── .github/
    └── workflows/
        ├── pr-quality-gate.yml      (stub)
        └── staging-smoke.yml        (stub)

Parent POM requirements:
- groupId: PACKAGE_BASE
- artifactId: PROJECT_NAME
- Java 17, UTF-8
- Modules: rest-assured, karate, pact
- Manage these dependency versions in dependencyManagement (never declare versions 
  in child POMs):
    - REST Assured 5.3.2
    - TestNG 7.9.0
    - com.intuit.karate:karate-junit5:1.4.0   (NOT io.karatelabs — not on Maven Central)
    - au.com.dius.pact.consumer:junit5:4.6.7
    - au.com.dius.pact.provider:junit5:4.6.7
    - junit-jupiter-api and junit-jupiter-engine (latest stable)
    - jackson-databind (latest stable)
    - logback-classic (latest stable)
    - SLF4J API (latest stable)
- Properties: api.base.url (default TARGET_API), pact.broker.url, pact.broker.token
- Maven Surefire 3.x configured to use JUnit platform in pluginManagement

README.md must have exactly 8 sections written as an architect explaining to a VP 
why Postman/Newman fails at platform scale and what this replaces it with:
1. The Problem This Solves — three specific Postman failure modes: governance, 
   secrets management, contract awareness
2. Architecture Decision — three layers, what each solves, why Maven multi-module
3. Layer 1: REST Assured — functional validation, ApiConfig pattern, system property injection
4. Layer 2: Karate — BDD governance, karate-config.js, why Karate over Cucumber
5. Layer 3: Pact — consumer-driven contracts, the guarantee it provides, Pact Broker
6. Pipeline Integration — two workflows, what runs when and why
7. What This Replaces and What It Doesn't — Postman narrows to exploration only
8. Getting Started — prerequisites, mvn commands for each layer

The README must include a concrete example in the Pact section: Patient API renames 
the "name" field to "fullName". Walk through what happens without contract testing 
(silent production failure) versus with contract testing (PR blocked, conversation 
between engineers).

Do not write tutorial-style content. Write as an architect, not an instructor.
```

---

### Prompt 2 — REST Assured Layer

```
Build out the complete REST Assured layer for PROJECT_NAME. 
All code must be production-quality — not demo quality.

Package structure under rest-assured/src/test/java/PACKAGE_BASE/api/:
  config/ApiConfig.java
  client/PatientApiClient.java
  client/PrescriptionApiClient.java
  client/CartApiClient.java
  PrescriptionApiTest.java
  CartApiTest.java

ApiConfig requirements:
- getBaseSpec() returns a NEW RequestSpecBuilder().build() on every call — not a 
  singleton. This is intentional: TestNG parallel execution requires thread-safe specs.
- Base URL from System.getProperty("api.base.url")
- Connection timeout: 5000ms, socket timeout: 10000ms
  Use string literals "http.connection.timeout" and "http.socket.timeout" — 
  RestAssuredConfig requires strings, not the enum.
- FailureOnlyLoggingFilter as a static inner class:
    - Implements Filter
    - In filter(): call next.next(requestSpec, responseSpec, ctx) to get the response
    - Log full request + response at ERROR level via SLF4J only when 
      response.getStatusCode() >= 400
    - Use Java 16+ pattern matching: if (body instanceof String s) return s;
    - Silent on success — no log noise in the passing case

API client pattern (enforce strict separation):
- Client classes: HTTP mechanics only. No assertions. No test logic. 
  Returns ValidatableResponse from every method.
- Test classes: assertions only. Never call RestAssured directly.

PatientApiClient → maps to /users endpoint:
  getPatient(int id) → GET /users/{id}
  getAllPatients() → GET /users
  createPatient(Map<String,Object> payload) → POST /users
  deletePatient(int id) → DELETE /users/{id}

PrescriptionApiClient → maps to /posts endpoint:
  getPrescriptionsForPatient(int patientId) → GET /posts?userId={patientId}
  submitPrescriptionRefill(Map<String,Object> payload) → POST /posts

CartApiClient → maps to /todos endpoint:
  getCartItemsForPatient(int patientId) → GET /todos?userId={patientId}
  addCartItem(Map<String,Object> payload) → POST /todos
  getAllCartItems() → GET /todos

PrescriptionApiTest (TestNG, @Test(groups="prescription") on class):
- @BeforeClass logs: [REST Assured] Starting PrescriptionApiTest against: {url}
- Test 1: getPatientRecord_validId_returns200WithSchema
    GET /users/1, assert 200, assert body has id/name/email/phone, 
    assert response.getTime() < 2000
- Test 2: getPatientRecord_invalidId_returns404
    GET /users/99999, assert 404
    Comment explaining why error-path testing matters in healthcare workflows
- Test 3: submitPrescriptionRefill_validPayload_returns201
    POST /posts with userId/title/body payload, assert 201, assert id > 0
- Test 4: prescriptionSubmission_responseTime_underSLA
    POST /posts, assert response.getTime() < 3000
    Comment explaining why SLA is a separate test, not folded into test 3

CartApiTest: similar structure, 3 tests.

rest-assured/src/test/resources/logback-test.xml:
- Set org.apache.http.wire to OFF
- Set io.restassured to WARN
- Suppress all wire-level HTTP logging in the passing case

rest-assured/pom.xml:
- Parent: PACKAGE_BASE:PROJECT_NAME
- Dependencies: rest-assured, testng, slf4j-api, logback-classic (all from parent 
  dependencyManagement, no versions in child POM)
- Surefire: include **/*Test.java, pass api.base.url as systemPropertyVariable

Write zero comments that explain what the code does. Only write a comment when the 
WHY is non-obvious — hidden constraints, workarounds, invariants a reader would miss.
```

---

### Prompt 3 — Karate Layer

```
Build out the complete Karate layer for PROJECT_NAME.
All feature files must be readable by a non-engineer — product managers and QA 
leads must be able to read scenario names and understand what is being validated 
without reading the step bodies.

File structure under karate/src/test/resources/:
  karate-config.js
  features/
    DOMAIN/
      DOMAIN-api.feature

karate-config.js requirements:
- Three environments: dev, staging, prod
- dev: baseUrl from System.getProperty("api.base.url", "TARGET_API")
- staging: https://staging-api.[your-domain].com
- prod: https://api.[your-domain].com
- For staging and prod: if api.base.url system property is set, it wins (enables 
  CI override without code changes)
- Log the resolved environment and baseUrl: 
  karate.log('[Karate] Environment:', env, '| Base URL:', config.baseUrl)
- No credentials in this file. Credentials come from environment variables 
  injected by the pipeline.

Feature file requirements:
- @DOMAIN tag on the feature (not individual scenarios)
- @smoke tag on scenarios that must pass as a deployment health check
- Scenario names must be business language — written as if Jason (a non-engineer 
  product manager) will read the HTML report and needs to understand what failed
  without asking an engineer

DOMAIN-api.feature must include:
1. @smoke Scenario: Retrieve known patient record by ID
   GET /users/1
   Assert status 200
   Assert response.id == 1 (exact match, not type check)
   Assert response.name == 'Leanne Graham' (exact match)
   Assert response has email and phone fields

2. @smoke Scenario: Retrieve non-existent patient returns 404
   GET /users/99999, assert status 404

3. @smoke Scenario: Write prescription refill for existing patient
   POST /posts with a payload, assert status 201, assert id is present

4. Scenario Outline: Invalid patient IDs return client errors
   IDs: 0, -1, 99999
   Assert responseStatus >= 400
   Add a comment above the Outline explaining why boundary values matter in a 
   medication dispensing workflow — this is the non-obvious WHY

KarateRunner.java under karate/src/test/java/PACKAGE_BASE/karate/:
- JUnit 5 @Test void testParallel()
- Runner.path("classpath:features").parallel(5)
- Read tags from System.getProperty("karate.options", "--tags @smoke")
- Strip the "--tags " prefix before passing to .tags()
- Fail the build via assertEquals(0, results.getFailCount(), results.getErrorMessages())

karate/pom.xml:
- com.intuit.karate:karate-junit5:1.4.0 (from parent dependencyManagement)
- Also explicitly declare junit-jupiter-api and junit-jupiter-engine 
  (Karate needs them resolvable at test runtime)
- Surefire: include **/*Runner.java only

Write a cart feature file with the same structure: @cart @smoke tags, 3 scenarios, 
business language names, one scenario validating Content-Type response header 
using Karate's match syntax on responseHeaders.
```

---

### Prompt 4 — Pact Contract Testing

```
Build the Pact consumer-driven contract testing layer for PROJECT_NAME.
This is the most architecturally significant layer. Explain it clearly in code 
comments — this is the layer interviewers and VP-level stakeholders will scrutinize.

File structure under pact/src/test/java/PACKAGE_BASE/pact/:
  consumer/PrescriptionConsumerTest.java  (CONSUMER_NAME consuming PROVIDER_NAME)
  provider/PatientProviderTest.java       (PROVIDER_NAME verifying contracts)

PrescriptionConsumerTest requirements:
- @ExtendWith(PactConsumerTestExt.class)
- @PactTestFor(providerName = "PROVIDER_NAME", pactVersion = PactSpecVersion.V3)
  V3 is explicit and required. Pact 4.6.7 defaults to V4 which requires a different 
  PactBuilder DSL and breaks the familiar RequestResponsePact API.
- @Pact(consumer = "CONSUMER_NAME") on the @Pact method — returns RequestResponsePact
- Use PactDslWithProvider builder pattern
- PactDslJsonBody declares MINIMUM fields only:
    .integerType("id", 1)
    .stringType("name", "Leanne Graham")
    .stringType("email", "Sincere@april.biz")
    .stringType("phone", "1-770-736-8031 x56442")
  Comment explaining why minimum fields: allows provider to add/restructure 
  without triggering false violations
- State: "PROVIDER_NAME user with ID 1 exists"
- Interaction: GET /users/1 → 200 + body
- @Test method uses Java 11 HttpClient (no REST Assured dependency in pact module)
  Calls mockServer.getUrl() + "/users/1", asserts 200, asserts body not null

Class-level Javadoc must explain the 5-step contract testing flow in plain English:
  1. @Pact method declares the interaction
  2. Pact starts a mock server — consumer never touches the real provider
  3. @Test proves the consumer can make the request and handle the response
  4. Pact writes the verified interaction to target/pacts/CONSUMER_NAME-PROVIDER_NAME.json
  5. PatientProviderTest loads that file and replays it against the real provider

PatientProviderTest requirements:
- @Provider("PROVIDER_NAME")
- @PactFolder("target/pacts")
- @BeforeEach configureTarget(PactVerificationContext context):
    Read api.base.url system property
    Use URL.getProtocol() to pick HttpsTestTarget.fromUrl(url) for https or 
    HttpTestTarget.fromUrl(url) for http
    Use the static fromUrl() factory — do NOT use the constructor. The constructor 
    signature changed in 4.6.x and will cause a compilation error.
- @State("PROVIDER_NAME user with ID 1 exists") — no-op body, comment explaining why
- @TestTemplate @ExtendWith(PactVerificationInvocationContextProvider.class) 
  void verifyContracts(PactVerificationContext context) → context.verifyInteraction()

pact/pom.xml — TWO ordered Surefire executions (this is the critical config):
  Problem: alphabetical ordering puts PatientProviderTest before 
  PrescriptionConsumerTest. Provider verification fails because the pact JSON 
  doesn't exist yet.
  Solution:
  1. Disable the default-test execution by binding it to phase: none
  2. Add execution consumer-contract-generation: includes **/*ConsumerTest.java
  3. Add execution provider-contract-verification: includes **/*ProviderTest.java
  Maven executes them in declaration order within the same phase.

  All executions must inherit:
    <useModulePath>false</useModulePath>
    <pact.rootDir>${project.build.directory}/pacts</pact.rootDir>

In the CI workflow, when filtering with -Dtest=ClassName and the module has multiple 
Surefire executions, add -DfailIfNoSpecifiedTests=false. Without it, Surefire 3.x 
fails the build when a filtered execution finds no matching tests.

Do not add a comment summarizing what the code does. Only add comments for 
non-obvious constraints: the V3 requirement, the fromUrl() factory requirement, 
the ordering problem and its solution.
```

---

### Prompt 5 — GitHub Actions Pipeline

```
Build the two GitHub Actions workflow files for PROJECT_NAME.
These are the most important files in the repository after the README. 
Every comment must explain WHY, not WHAT.

.github/workflows/pr-quality-gate.yml requirements:

Trigger: pull_request → branches: [main, develop]

Workflow-level env:
  JAVA_HOME: ${{ env.JAVA_HOME }}   (documents the Java dependency; evaluates to 
                                     empty at parse time — this is intentional)
  JAVA_VERSION: '17'
  API_BASE_URL: 'TARGET_API'

Permissions: checks: write, pull-requests: write
(required for dorny/test-reporter to post Check annotations on the PR)

Four jobs with this exact dependency structure:
  Job 1 — rest-assured-functional (no dependencies)
    name: REST Assured — Functional API Validation
    steps: checkout, setup-java (temurin, cache: maven), 
    mvn test -pl rest-assured -Dapi.base.url=${{ env.API_BASE_URL }}
    dorny/test-reporter@v1: reporter java-junit, 
      path rest-assured/target/surefire-reports/TEST-*.xml,
      fail-on-error: 'false'
      (fail-on-error is false because Maven exit code already controls job 
       pass/fail — reporter should annotate, not double-fail)

  Job 2 — karate-bdd (no dependencies, runs in parallel with Job 1)
    name: Karate — BDD API Scenarios
    steps: checkout, setup-java,
    mvn test -pl karate -Dapi.base.url=${{ env.API_BASE_URL }}
    upload-artifact@v4: karate/target/karate-reports/ as karate-report

  Job 3 — pact-consumer (needs: [rest-assured-functional, karate-bdd])
    name: Pact — Consumer Contract Generation
    steps: checkout, setup-java,
    mvn test -pl pact \
      -Dtest=PrescriptionConsumerTest \
      -DfailIfNoSpecifiedTests=false \
      -Dapi.base.url=${{ env.API_BASE_URL }}
    upload-artifact@v4: pact/target/pacts/ as pact-contracts
    Comment on the needs: dependency — explain why contracts must not be generated 
    from a failing codebase. A contract from broken code encodes the bug as a 
    requirement and trains the provider to satisfy incorrect expectations.

  Job 4 — pact-provider (needs: [pact-consumer])
    name: Pact — Provider Contract Verification
    steps: checkout, setup-java,
    download-artifact@v4: restore pact-contracts to pact/target/pacts/
    (path must match @PactFolder("target/pacts") — Surefire sets working 
     directory to module root, so target/pacts → pact/target/pacts/ from repo root)
    mvn test -pl pact \
      -Dtest=PatientProviderTest \
      -DfailIfNoSpecifiedTests=false \
      -Dapi.base.url=${{ env.API_BASE_URL }}

.github/workflows/staging-smoke.yml requirements:

Trigger: workflow_dispatch ONLY
  Remove repository_dispatch. Remove PagerDuty. Remove workflow inputs.
  This workflow is manually triggered by the deployment pipeline.

Workflow-level env: same JAVA_HOME pattern, JAVA_VERSION, API_BASE_URL

One job — staging-smoke:
  name: Karate — Staging Smoke Scenarios
  steps: checkout, setup-java,
  mvn test -pl karate \
    -Dkarate.options="--tags @smoke" \
    -Dapi.base.url=${{ env.API_BASE_URL }}
  upload-artifact@v4: karate/target/karate-reports/ as staging-smoke-report
  Comment on the upload: HTML report surfaces which scenario failed and what the 
  actual response was. Engineers triaging a failed smoke gate read this first.

Comment at the top of staging-smoke.yml:
"Smoke test runs after every deployment to staging. @smoke tag = critical paths only.
5 minutes maximum. If this fails, staging is unhealthy — stop all deployments until fixed."

REST Assured and Pact do not run here. Code was validated on PR before the artifact 
was promoted. Running them again validates infrastructure, not code.
```

---

### Prompt 6 — Architecture Documentation

```
Rewrite docs/architecture-decision.md and docs/pipeline-design.md.
Both documents must be sharp, peer-level, and concise — written as an architect 
explaining decisions to a VP peer. No tutorials. No hand-holding. Under one page each.

architecture-decision.md must cover:

1. Context — what Postman/Newman fails at, specifically. Three failure modes as 
   named paragraphs: governance (40 engineers, 12 squads, no review gate), secrets 
   (plaintext JSON, HIPAA/PCI environment), contract visibility (Newman cannot detect 
   schema drift between services).

2. Decision — three layers, one sentence each on what problem each solves:
   REST Assured: deep payload validation, auth flows, stateful sequences requiring Java
   Karate: governed BDD scenarios reviewable by non-engineers; test changes are 
     PR-gated artifacts
   Pact: consumer-driven contracts that surface schema drift at PR time, not production

3. Alternatives considered — prose sentences with the disqualifying reason embedded 
   (not a table):
   Postman only: governance and secrets are architectural — no tooling layer fixes them
   Playwright API only: TypeScript-first, no Pact broker integration, adds runtime 
     heterogeneity for no gain
   Karate only: no native Pact broker integration; OAuth2/mTLS interop is fragile

4. Trade-offs — honest, one line per layer:
   REST Assured: non-Java QA engineers cannot own these tests without ramp-up
   Karate: scope it to what it does well; Java interop at the edges is fragile
   Pact: requires Pact Broker infrastructure before cross-team guarantees apply

5. What this replaces — Postman role narrows to exploration and documentation only. 
   Removed from CI entirely. Newman removed from CI entirely.

6. When to evolve:
   TypeScript org: evaluate Pact JS + Playwright, three-layer model holds
   Java expertise gaps: shift coverage to Karate, reduce REST Assured to auth 
   flows and SLA validation only

pipeline-design.md must cover:

1. The principle — one declarative statement. A test in the wrong gate is quality 
   theater. Every gate has an explicit rationale.

2. On Pull Request — table: job, what it validates, why here.

3. On Merge — one paragraph. The right answer is: nothing additional runs. 
   The PR gate is the quality gate. Running the same tests again on merge validates 
   the pipeline, not the code.

4. On Staging Deploy — Karate @smoke only. Validates the deployment 
   (DNS, ingress, service mesh, secrets injection) — not the code.

5. The dependency chain — ASCII diagram showing the parallel/serial structure. 
   Explain why the chain is a correctness constraint, not a convenience: a contract 
   from broken code encodes the bug as a requirement.

6. Coverage intelligence — SeaLights as the next layer (reference only, not 
   implemented). Explain what it answers that this pipeline cannot. Note that 
   integration requires tagging test results with build metadata in each job.
```

---

### Prompt 7 — Validation and Cleanup

```
Validate the complete PROJECT_NAME build and fix any failures.

Run in this order:
1. mvn clean compile -DskipTests
   Fix any compilation errors before proceeding.

2. mvn test -pl rest-assured -Dapi.base.url=TARGET_API
   All tests must pass. Fix failures before proceeding to next module.

3. mvn test -pl karate -Dapi.base.url=TARGET_API
   All @smoke scenarios must pass.

4. mvn test -pl pact -Dapi.base.url=TARGET_API
   Consumer test must generate target/pacts/CONSUMER_NAME-PROVIDER_NAME.json
   Provider test must pass verification against TARGET_API.
   Confirm the pact JSON file exists: ls pact/target/pacts/

5. Validate GitHub Actions YAML syntax:
   python3 -c "import yaml; yaml.safe_load(open('.github/workflows/pr-quality-gate.yml'))"
   python3 -c "import yaml; yaml.safe_load(open('.github/workflows/staging-smoke.yml'))"
   Both must parse without error.

Known issues to watch for:
- Pact V4 vs V3 mismatch: if you see "Method does not conform required method signature 
  'public V4Pact xxx(PactBuilder builder)'" — add pactVersion = PactSpecVersion.V3 
  to the @PactTestFor annotation.
- HttpTestTarget constructor error: use HttpTestTarget.fromUrl(URL) static factory, 
  not the constructor. The constructor signature changed in Pact 4.6.x.
- Surefire ordering: if PatientProviderTest runs before PrescriptionConsumerTest, 
  the pact JSON does not exist yet. Fix by disabling default-test execution and 
  adding two named executions in declaration order.
- karate groupId: use com.intuit.karate:karate-junit5:1.4.0, NOT io.karatelabs — 
  io.karatelabs is not on Maven Central.
- -DfailIfNoSpecifiedTests=false: required in CI when using -Dtest=ClassName with 
  a module that has multiple Surefire executions.

After all tests pass:
- git add -A
- git commit -m "test: all layers passing against TARGET_API"
- Confirm git log shows a clean linear history with one commit per layer
```

---

### Verification Checklist

- [ ] REST Assured: 0 assertions in client classes — clients return `ValidatableResponse`, assertions live only in test classes
- [ ] Karate: feature files readable by a non-engineer — scenario names describe business outcomes, not HTTP operations
- [ ] Pact: consumer contract JSON generated at `pact/target/pacts/CONSUMER_NAME-PROVIDER_NAME.json`
- [ ] Pact: provider verification calls the real API (check logs — no mock server in provider test)
- [ ] Pipeline: 4-job dependency chain correct — jobs 1+2 parallel, job 3 waits on both, job 4 waits on job 3
- [ ] Pipeline: pact artifact download path in job 4 matches `@PactFolder("target/pacts")`
- [ ] All tests passing locally before any commit
- [ ] YAML syntax valid for both workflow files
- [ ] Git log shows clean linear commit history — one commit per layer, no fixup commits visible

---

### How to Present This to a Technical VP

Five talking points. No notes needed.

**1. Three problems with Postman at enterprise scale**
Postman breaks in three specific ways at platform scale: governance (test changes ship without review because collections live in personal accounts), secrets (credentials exported as plaintext JSON — a HIPAA violation waiting to happen), and contract blindness (Newman cannot tell you when a provider team's schema change breaks every downstream consumer). This platform solves all three, each with the right tool.

**2. Why client/test separation matters**
REST Assured client classes contain zero assertions. They return `ValidatableResponse`. Test classes contain zero HTTP configuration. This is the API equivalent of the Page Object Model. When an endpoint changes, you update one client class. When an assertion changes, you update one test class. Engineers who violate this boundary create maintenance debt that compounds across every test class that touches that endpoint.

**3. What Pact solves that functional testing cannot**
A Karate scenario that asserts `status 200` and checks a few fields will pass even if the provider renames a field your consumer depends on. Pact inverts the testing direction: the consumer declares what minimum shape it needs, the provider is required to satisfy that declaration before merging. The Patient API team cannot rename `name` to `fullName` without first failing `PatientProviderTest` on their own PR. The conversation happens between two engineers before a single line ships to staging.

**4. Pipeline gate logic — what runs when and why**
Functional tests run on PR because they're fast and scoped to the changed service. Pact consumer runs after functional tests because a contract generated from broken code encodes the bug as a requirement. Pact provider runs after consumer because the pact JSON artifact must exist. Staging smoke runs post-deploy to validate the deployment itself — DNS, ingress, secrets injection — not the code, which was already validated before the artifact was promoted.

**5. How to answer "did you write this?"**
Yes. Walk through these specifics: the `FailureOnlyLoggingFilter` in `ApiConfig` is a static inner class that logs at ERROR only when `response.statusCode() >= 400` — silent in the passing case. The Pact V3 format is explicit because Pact 4.6.7 defaults to V4 which requires a different DSL. The two ordered Surefire executions in `pact/pom.xml` exist because alphabetical ordering puts `PatientProviderTest` before `PrescriptionConsumerTest` — provider verification would fail on a missing artifact. The `-DfailIfNoSpecifiedTests=false` flag in CI is required when filtering by class name across a multi-execution Surefire config. These are not decisions you make by reading documentation — they come from debugging a real build.

---

### Customization Guide

**TypeScript stack (swap REST Assured for Playwright API)**
Replace the `rest-assured` module with a Node.js module using Playwright's `APIRequestContext`. Keep Karate for BDD scenarios (Karate runs on JVM but can be used alongside a TypeScript build). Replace Pact JVM with `@pact-foundation/pact` (same consumer-driven model, TypeScript DSL). The GitHub Actions jobs stay structurally identical — only the runtime and test commands change.

**Python stack (swap REST Assured for Requests + pytest)**
Replace `rest-assured` with a Python module using `requests` and `pytest`. Use `pytest-pact` for contract testing. Karate can stay as the BDD governance layer (cross-language capability is a Karate strength). The client/test separation pattern applies directly: `client.py` classes contain no assertions, `test_*.py` files contain no HTTP configuration.

**Non-Java teams (Karate becomes primary layer)**
If the QE team cannot maintain Java code, collapse REST Assured into Karate. Karate's native HTTP client handles most functional validation scenarios. Retain Pact — it is the highest-value layer and the one with no adequate alternative. Limit Java ownership to `KarateRunner.java` and the Pact test classes; everything else is `.feature` files and `karate-config.js`.

**Adding k6 performance layer**
Add a fourth module `k6/` at the root. k6 scripts are JavaScript, so this module holds the scripts and a Maven exec plugin that invokes the k6 binary. Add a fifth job to `pr-quality-gate.yml` — `k6-performance` — that runs after `rest-assured-functional` with a threshold: p95 < 500ms. This gate runs on PR, not post-deploy, for the same reason Pact runs on PR: catching a regression before merge costs a conversation, catching it post-deploy costs a rollback.
