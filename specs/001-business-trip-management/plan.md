# Implementation Plan: Business Trip Management

**Branch**: `001-business-trip-management` | **Date**: 2026-09-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-business-trip-management/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Employees sign in with the organization's identity provider and book one of three pool cars
for a business trip. They enter the period first, see which cars are free, pick a car, and
enter the destination. They see their own trips in a list sorted by start, and can edit or
delete trips that have not started yet. The system prevents double booking under concurrent
use and deletes trips automatically after a configurable retention period.

Technical approach (user input: **Bootstrap 5 for the frontend**): one server-rendered
Spring Boot application with Thymeleaf templates styled with self-hosted Bootstrap 5.3. The
three-step wizard uses plain HTML forms, and every rule is validated on the server. Data is
stored in PostgreSQL, where an exclusion constraint on `(car_id, tstzrange(start, end))`
makes double booking impossible. Login uses OIDC. The app runs as a stateless container on
Kubernetes with health probes, structured logs, and OpenTelemetry. Details:
[research.md](./research.md).

## Technical Context

**Language/Version**: Java 25 (LTS)

**Primary Dependencies**: Spring Boot 4.0.x (Web MVC, Thymeleaf, Security + OAuth2 Client, Data JPA, Validation, Actuator, Session JDBC), Bootstrap 5.3.x (WebJar, self-hosted), Flyway, Micrometer/OpenTelemetry

**Storage**: PostgreSQL 17 (`btree_gist` exclusion constraint for overlap prevention; Spring Session tables)

**Testing**: JUnit 5, AssertJ, Spring Boot Test + MockMvc + Spring Security Test, Testcontainers (PostgreSQL, Keycloak), Playwright for Java + axe-core, ArchUnit

**Target Platform**: Linux container (OCI) on Kubernetes, deployed via Helm; modern evergreen browsers (desktop and mobile)

**Project Type**: Web application (server-rendered, single deployable)

**Performance Goals**: List of 500 trips rendered < 2 s (SC-003); p95 page response < 500 ms under normal load

**Constraints**: WCAG 2.1 AA / BITV 2.0; German UI and date format, Europe/Berlin; no third-party CDN (DSGVO); strict CSP; stateless pods; zero double bookings under concurrency

**Scale/Scope**: One organization, up to low thousands of users, 3 cars, ~500 trips per user; 5 screens (list, 3 wizard steps, delete confirmation)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Pre-research | Post-design | Evidence |
|-----------|------|--------------|-------------|----------|
| I. Human-Centered Design | Users, goals, testable scenarios in spec | ✅ | ✅ | spec US1–US4 with acceptance scenarios |
| | WCAG 2.1 AA / BITV 2.0 | ✅ (required by FR-016) | ✅ | research R3 (Bootstrap a11y measures), R4 (native inputs); axe + manual audit in quickstart #9 |
| | Plain language, actionable errors | ✅ | ✅ | German message catalogue V1–V11 in data-model.md; error summary pattern in contracts/ui-routes.md |
| | No re-entry of known data | ✅ | ✅ | Wizard keeps values on back/errors; edit pre-filled; identity from IdP |
| II. TDD (non-negotiable) | Acceptance criteria → automated tests before code | ✅ | ✅ | research R9 test levels; every contract route + rule is a test case; `Clock` injection for deterministic time tests |
| | Unit, integration, E2E; runs in CI | ✅ | ✅ | JUnit / Testcontainers / Playwright; `./mvnw verify` in quickstart |
| III. Cloud Native | OCI container, declarative deploy, IaC | ✅ | ✅ | Dockerfile + Helm chart (R11) |
| | Twelve-Factor: env config, stateless | ✅ | ✅ | contracts/configuration.md; sessions in DB, wizard state in forms |
| | Health endpoints, graceful shutdown | ✅ | ✅ | Actuator liveness/readiness on management port |
| | Observability (open standards) | ✅ | ✅ | JSON logs, Micrometer + OTLP |
| | Vendor neutrality | ✅ | ✅ | Only open-source components; standard PostgreSQL and OIDC, no cloud-specific services |
| | Builds and deployments fully automated via CI/CD; infrastructure as code; PR review | ⚠️ deferred | ⚠️ deferred | Pipeline scripts in `ci/` and the Helm chart are delivered, but no pipeline definition, automated deployment or database provisioning. See Complexity Tracking |
| IV. Shift-Left Security | Data, protection needs, threats identified; GDPR | ✅ (spec section) | ✅ | Data minimization (only `sub` stored), retention job (R7), user deletion (FR-021) |
| | Security as automated tests | ✅ | ✅ | Ownership, read-only, CSRF, no-disclosure (FR-019), injection tests |
| | SAST, SCA, secret detection, SBOM in CI | ✅ | ✅ | Semgrep/FindSecBugs, Dependency-Check/Trivy, gitleaks, CycloneDX (R10) |
| | Secrets from secret store | ✅ | ✅ | Only env/K8s Secrets, none in repo or image |
| | AuthN/AuthZ on every endpoint, input validation | ✅ | ✅ | OIDC on all routes; owner-scoped queries; server-side validation V1–V11 |

**Result**: All gates pass except the CI/CD automation gate of Principle III. It is deliberately deferred (decision 2026-09-27) and justified in Complexity Tracking; reviewers must approve the deferral.

**Open non-blocking items** (business input, needed before go-live, not for development):
concrete car names/plates, retention period value (DPO), IdP client registration. The app
refuses to start in `prod` without them, so they cannot be forgotten.

## Project Structure

### Documentation (this feature)

```text
specs/001-business-trip-management/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   ├── ui-routes.md     # HTTP routes, forms, page content, messages
│   └── configuration.md # Environment variables / operator contract
└── tasks.md             # Phase 2 output (/speckit-tasks – not created here)
```

### Source Code (repository root)

```text
pom.xml
mvnw, .mvn/
Dockerfile
compose.dev.yaml                       # PostgreSQL + Keycloak for local dev
dev/keycloak/realm-dienstreise.json    # dev realm with test users

src/main/java/de/lgln/dienstreise/
├── DienstreiseApplication.java
├── config/            # ClockConfig, SecurityConfig (OIDC, CSP, CSRF), DienstreiseProperties
├── car/               # Car entity, CarRepository, CarCatalogInitializer (config → DB reconcile)
├── trip/
│   ├── domain/        # BusinessTrip entity, TripPeriod value object, TripStatus, exceptions
│   ├── TripRepository.java        # owner-scoped queries, availability query
│   ├── TripService.java           # create/update/delete, validation V1–V11, 23P01 translation
│   ├── AvailabilityService.java
│   └── web/           # TripListController, TripWizardController, TripDeleteController, form DTOs
├── retention/         # RetentionJob (+ metric)
└── web/               # CurrentUser resolver, DateTimeFormatting, global error handling

src/main/resources/
├── application.yaml, application-dev.yaml, application-prod.yaml
├── messages.properties            # German UI texts and validation messages
├── db/migration/                  # V1__car.sql, V2__business_trip.sql, V3__spring_session.sql
├── templates/
│   ├── layout.html                # Bootstrap 5 shell: skip link, header, nav, main, footer
│   ├── fragments/                 # error-summary, flash, trip-summary, form-field
│   └── trips/                     # list, period, car, destination, delete-confirm
└── static/css/app.css             # Bootstrap overrides (focus, contrast)

src/test/java/de/lgln/dienstreise/
├── unit/              # TripPeriod, validation, DST/format, retention cutoff
├── integration/       # MockMvc route contract, ownership, read-only, concurrency, Flyway, retention
├── e2e/               # Playwright journeys US1–US4 + axe accessibility checks
└── architecture/      # ArchUnit rules

deploy/helm/dienstreise/            # Chart: Deployment, Service, Ingress, probes, Secret refs, values per env
ci/                                 # Platform-neutral pipeline scripts (build, test, scan, SBOM, image)
```

**Structure Decision**: Single project. The UI is rendered on the server, so there is no
separate frontend project. Bootstrap 5 is served as a Maven WebJar, so no Node toolchain is
needed. Code is organized by domain (`car`, `trip`, `retention`), with the web layer inside
each domain. The CI definition file (GitLab CI or GitHub Actions) will be added once the
repository remote and platform are known; it calls the scripts in `ci/`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| CI/CD pipeline and automated deployment deferred (Principle III: "Builds and deployments MUST be fully automated through CI/CD pipelines"; Development Workflow: CI must pass before merge) | The repository has no remote and the CI/CD platform (GitLab CI or GitHub Actions) and target cluster are not yet decided (decision by the product owner, 2026-09-27). All gates are prepared as platform-neutral scripts in `ci/` (tests, SAST, SCA, secret detection, SBOM, image scan, Helm lint) and must be run locally before every merge. | Writing a pipeline for a guessed platform would produce unused or wrong configuration. **Follow-up (mandatory before the first deployment to a shared environment)**: add the pipeline definition calling `ci/*.sh`, automated `helm upgrade` deployment, and PR protection on `main`. |
| Database provisioning not defined as code (Principle III: "infrastructure MUST be defined as code") | Depends on the same platform decision (managed PostgreSQL vs. operator in the cluster). The application's own infrastructure (container, Helm chart) is code. | Same as above; to be added with the pipeline follow-up. |
