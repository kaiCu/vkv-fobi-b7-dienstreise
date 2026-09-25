# Research: Business Trip Management

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-09-27

This document resolves every open point from the plan's Technical Context. The only
stack decision given by the user is **Bootstrap 5 for the frontend**; all other decisions
below were derived from the specification and the constitution.

---

## R1. Application architecture & rendering model

- **Decision**: Single server-rendered web application (Spring Boot + Thymeleaf) styled with
  Bootstrap 5. Every step of the trip wizard is a plain HTML form submitted to the server
  (POST-Redirect-GET). JavaScript is optional progressive enhancement only.
- **Rationale**:
  - All validation must happen on the server anyway (FR-008, FR-006a, FR-017, FR-023);
    server rendering avoids duplicating logic in a client app.
  - Plain HTML forms with native controls are the most robust basis for WCAG 2.1 AA /
    BITV 2.0 (FR-016): keyboard operability, focus handling and screen-reader semantics
    come for free.
  - One deployable, one container, one pipeline — lowest operational effort for a small
    internal tool, while still satisfying Cloud Native (Principle III).
  - Bootstrap 5 works without a JS framework; no build toolchain (Node) is needed.
- **Alternatives considered**:
  - SPA (React/Vue/Angular) + REST API: two deployables, duplicated validation, larger
    attack surface (token handling in browser), more a11y effort (focus management on
    route changes). Not justified by four screens.
  - htmx-enhanced server rendering: nice for live availability updates, but not needed;
    can be added later without changing the architecture.

## R2. Language, framework & build

- **Decision**: Java 25 (LTS), Spring Boot 4.0.x (Spring Framework 7, Spring Security 7),
  Thymeleaf 3.1, Maven 3.9 with Maven Wrapper.
- **Rationale**: Java/Spring is the established stack in German public administration
  (skills, operations know-how, security reviews). Spring Security provides OIDC login,
  CSRF protection and security headers out of the box. Spring Boot provides Actuator
  health probes, structured logging and OpenTelemetry support (Principle III).
- **Alternatives considered**: Python/Django (equally suitable, less common in the target
  organization), Node/NestJS (would pull a JS toolchain into a server-rendered app),
  .NET/ASP.NET Core (fine technically, weaker fit with the Linux/Kubernetes and Java
  operations landscape).

## R3. Bootstrap 5 integration

- **Decision**: Bootstrap **5.3.x**, served **self-hosted** from the application via the
  `org.webjars.npm:bootstrap` WebJar (version pinned in `pom.xml`, scanned by SCA). Only
  `bootstrap.min.css` and `bootstrap.bundle.min.js` are used; project-specific overrides
  live in `static/css/app.css`. No CDN.
- **Rationale**:
  - DSGVO: loading assets from a third-party CDN transmits user IP addresses to that
    provider; self-hosting avoids this.
  - Allows a strict Content Security Policy (`default-src 'self'`, no inline scripts/styles).
  - Maven-managed WebJar is pinned, updated by Renovate/Dependabot and covered by
    dependency scanning (Principle IV).
- **Accessibility measures on top of Bootstrap** (Bootstrap alone is not AA-compliant):
  - Use `lang="de"`, a skip link (`visually-hidden-focusable`), landmark elements
    (`header`, `nav`, `main`), one `h1` per page, and a page `<title>` that reflects the
    wizard step and error state (e.g. "Fehler: Zeitraum wählen – Dienstreise").
  - Forms: every control has a visible `<label>`; required fields marked in text
    ("Pflichtfeld"), not only by colour or asterisk. Invalid fields get `is-invalid`,
    `aria-invalid="true"` and `aria-describedby` pointing to the `invalid-feedback` text.
    On a failed submit an **error summary** (`alert alert-danger`, `role="alert"`,
    `tabindex="-1"`, focused on load) lists all errors as links to the fields.
  - Car selection: `fieldset`/`legend` with radio buttons (`form-check`). Unavailable cars
    are rendered with `disabled` **and** the visible text "nicht verfügbar" in the label,
    so both sighted and screen-reader users get the information (US1 AS3).
  - Success confirmations use `alert alert-success` with `role="status"`.
  - Delete confirmation is a **dedicated confirmation page** (works without JS, focus is
    naturally at page start); no Bootstrap modal needed.
  - Colours: keep Bootstrap 5.3 defaults that meet 4.5:1; avoid `.text-body-tertiary`,
    `.opacity-*` for text, and placeholder-only hints. Verified by automated axe checks
    plus a manual BITV audit (SC-006).
  - Responsive layout via Bootstrap grid; zoom to 400 % / reflow at 320 px must work
    (WCAG 1.4.10).
- **Alternatives considered**: CDN (rejected: DSGVO, CSP); npm + Sass build to customise
  Bootstrap (rejected for now: adds Node toolchain; CSS overrides are sufficient);
  KERN UX-Standard / Designsystem of the Land (worth evaluating later; user asked for
  Bootstrap 5).

## R4. Date/time input and display (FR-015)

- **Decision**: Separate native inputs per point in time: `<input type="date">` and
  `<input type="time">` for start and for end (four fields). The server interprets the
  values as local time in **Europe/Berlin**, converts to an `Instant` and stores it as
  `timestamptz`. All output is formatted server-side with
  `DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMANY)` in Europe/Berlin.
- **DST handling**: ambiguous local times (autumn change) resolve to the earlier offset;
  non-existent local times (spring gap) shift forward, as `ZonedDateTime.of` does.
  The confirmation/review step always shows the resolved time, so the user sees the result.
- **"Now"**: every "is in the future / has started" check uses an injected
  `java.time.Clock` (FR-006a, FR-023 "server's current time"), which makes the tests
  deterministic (Principle II).
- **Rationale**: Native date/time controls are keyboard- and screen-reader-accessible and
  display in the browser/OS locale (German for the target users). Splitting date and time
  is more accessible than `datetime-local`, whose support by assistive technology is uneven.
- **Alternatives considered**: `datetime-local` (a11y inconsistencies); JS date picker
  libraries (a11y risk, extra dependency); free-text fields `TT.MM.JJJJ` (more input
  errors, needs parsing).

## R5. Persistence and double-booking prevention (FR-017, FR-018, SC-007)

- **Decision**: PostgreSQL 17 with Flyway migrations and Spring Data JPA. Double booking
  is prevented **by the database** with an exclusion constraint:

  ```sql
  CREATE EXTENSION IF NOT EXISTS btree_gist;
  ALTER TABLE business_trip ADD CONSTRAINT business_trip_no_overlap
    EXCLUDE USING gist (car_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&);
  ```

  The application checks availability first (to show "nicht verfügbar" and give friendly
  messages), but the constraint is the final authority. A violation
  (SQLSTATE `23P01`) is translated into the domain error `CarAlreadyBookedException`, which
  sends the user back to the car selection with updated availability (US4 AS2).
- **Rationale**: A half-open range `[start, end)` exactly implements "back-to-back allowed"
  (US4 AS3). The constraint is atomic under any concurrency and across multiple pods,
  without application locks (US4 AS7, FR-018). Editing a trip updates the same row, so a
  trip never conflicts with itself (US4 AS5).
- **Addendum (implementation, 2026-09-27)**: the concurrency test (SC-007) showed that
  concurrent inserts checked against the exclusion constraint can **deadlock** in PostgreSQL
  (SQLSTATE `40P01`) instead of failing with `23P01`. Retrying did not converge under 10
  parallel bookers. Bookings of the same car are therefore serialized with a
  transaction-scoped advisory lock (`pg_advisory_xact_lock(namespace, car_id)`) taken before
  the availability check (`CarBookingLock`). This works across pods, does not affect other
  cars, and the exclusion constraint remains the final authority.
- **Lost updates on edit**: optimistic locking with a `version` column; a stale edit form
  is rejected with a message asking the user to reload.
- **Alternatives considered**: `SELECT … FOR UPDATE` / serializable transactions (more
  complex, retries needed, easy to get wrong); application-level locks (break with
  multiple replicas); a slot table (overkill for arbitrary periods).

## R6. Authentication, authorization & sessions (FR-001, FR-001a, FR-013, FR-022)

- **Decision**: OpenID Connect login against the organization's identity provider
  (Spring Security `oauth2Login`). Keycloak is used locally and in integration tests.
  The user is identified by the stable OIDC claims `iss` + `sub`; only that identifier is
  stored with a trip. The display name is read from the ID token for the page header and
  is **not** persisted (data minimization).
  - Single role: every authenticated user has the same rights; no role checks, no admin UI.
  - Ownership is enforced in the repository layer: every read/update/delete query is
    scoped by `owner_subject` (`findByIdAndOwnerSubject`). Trips of other users behave as
    "not found" (HTTP 404) to avoid revealing their existence.
  - Sessions are stored server-side in PostgreSQL via **Spring Session JDBC**, so
    application pods stay stateless and disposable (Twelve-Factor). Cookie: `Secure`,
    `HttpOnly`, `SameSite=Lax`; session id rotated on login; idle timeout configurable
    (default 30 min). Logout performs OIDC RP-initiated logout.
- **Rationale**: Reuses the existing sign-in (spec assumption), no passwords in the app,
  least privilege, testable authorization (SC-005).
- **Alternatives considered**: SAML (possible if the IdP requires it; Spring Security
  supports it, same design otherwise); JWT bearer tokens in the browser (not needed with
  server rendering; weaker session revocation); sticky sessions (not cloud native).

## R7. Automatic deletion after retention period (FR-024)

- **Decision**: A scheduled job inside the application (`@Scheduled`, default hourly, cron
  configurable) executes

  ```sql
  DELETE FROM business_trip WHERE end_at < :now - :retention
  ```

  The retention period is an ISO-8601 duration supplied by the environment variable
  `DIENSTREISE_RETENTION_PERIOD` (e.g. `P90D`). The application **fails to start** in the
  `prod` profile if it is missing, which turns the pending DPO decision into an explicit
  release gate. Deleted row counts are logged and exposed as a metric
  (`dienstreise.retention.deleted`).
- **Rationale**: The statement is idempotent, so running it concurrently in several pods is
  harmless — no distributed lock is required. No extra Kubernetes object, easy to test with
  an injected `Clock`.
- **Alternatives considered**: Kubernetes CronJob (extra image/entrypoint, separate
  deployment); ShedLock (unnecessary given idempotence); PostgreSQL `pg_cron` (extension
  may not be available on the managed database).

## R8. Configuration of the three pool cars (FR-002, FR-003)

- **Decision**: Cars are defined by operators through configuration
  (`DIENSTREISE_CARS_0_NAME`, `DIENSTREISE_CARS_0_LICENSE_PLATE`, … indices 0–2) bound to a
  validated `@ConfigurationProperties` record. On startup the application reconciles the
  `car` table by slot (1–3): names and plates are updated, rows are never deleted while
  trips reference them. Startup fails if not exactly three cars with non-blank, unique
  plates are configured.
- **Rationale**: Values come from the business owner shortly before go-live and must be
  changeable without a code change (spec assumption). Users have no car-management UI.
- **Alternatives considered**: Flyway seed data (would require a code change/new migration
  to change a plate); admin UI (explicitly out of scope).

## R9. Testing strategy (Principle II)

- **Decision**:
  | Level | Tooling | Covers |
  |-------|---------|--------|
  | Unit | JUnit 5, AssertJ, fixed `Clock` | Validation rules, period/overlap logic, DST conversion, formatting, retention cutoff |
  | Integration | `@SpringBootTest` + MockMvc + Spring Security Test (`oidcLogin()`), Testcontainers PostgreSQL | Routes contract, ownership (SC-005), read-only rules, CSRF, exclusion constraint, concurrent booking (SC-007), retention job, Flyway migrations |
  | End-to-end | Playwright for Java + Testcontainers Keycloak, `axe-core` Playwright integration | Critical journeys US1–US4 through a real browser, keyboard-only flow, automated WCAG 2.1 AA checks on every screen |
  | Architecture | ArchUnit | Controllers never access repositories directly; no `LocalDateTime.now()` / `Instant.now()` outside `Clock` |
- Every acceptance scenario in the spec maps to at least one automated test written before
  its implementation.
- **Concurrency test (SC-007)**: N threads concurrently save overlapping trips for the same car
  through the service; assert exactly one succeeds and the table has no overlaps.
- **Performance check (SC-003)**: integration test seeds 500 trips and asserts the list
  renders in < 2 s (guard, not a benchmark).
- **Alternatives considered**: H2 in-memory DB (rejected: no exclusion constraints/range
  types, would hide the core concurrency guarantee); Selenium (Playwright is faster and
  more reliable).

## R10. Security controls & pipeline (Principle IV, OWASP ASVS)

- **Decision**:
  - Spring Security defaults: CSRF tokens on all state-changing forms; security headers
    incl. strict **CSP** (`default-src 'self'; frame-ancestors 'none'; form-action 'self'`),
    HSTS, `Referrer-Policy: same-origin`, `X-Content-Type-Options`.
  - Output encoding by Thymeleaf (`th:text` only; `th:utext` forbidden, enforced by a
    test/lint rule). JPA with bound parameters only (injection via destination, spec threat).
  - Server-side validation with Jakarta Bean Validation plus domain checks (FR-008);
    destination trimmed, 1–200 characters, control characters rejected.
  - Unavailable cars cannot be forced by request manipulation: the car id is re-validated
    on every step and the constraint is the final guard.
  - Error pages never show stack traces; conflict messages never contain other users' data
    (FR-019, verified by tests).
  - CI: Maven build + tests, **Semgrep** + SpotBugs/FindSecBugs (SAST), **OWASP
    Dependency-Check** or **Trivy** for dependencies and container image (SCA), **gitleaks**
    (secret detection), **CycloneDX Maven plugin** for the SBOM. High/critical findings
    block the merge. Renovate keeps pinned dependencies current.
- **Rationale**: Covers the threats named in the spec (broken access control, information
  leakage via availability, request manipulation, injection, session hijacking).

## R11. Deployment & operations (Principle III)

- **Decision**: OCI image built with a multi-stage `Dockerfile` (Eclipse Temurin 25 JRE,
  non-root user, read-only root filesystem), deployed with a **Helm chart**
  (`deploy/helm/dienstreise`). PostgreSQL is an attached backing service (managed DB or
  operator-managed cluster), credentials injected from the organization's secret store as
  Kubernetes Secrets. Actuator exposes `/actuator/health/liveness` and
  `/actuator/health/readiness` on a separate management port; graceful shutdown enabled.
  Structured JSON logs (Spring Boot structured logging, ECS format) to stdout; metrics and
  traces via Micrometer + OpenTelemetry (OTLP exporter). Local development uses Docker
  Compose (PostgreSQL + Keycloak).
- **CI platform**: The repository has no remote yet. The pipeline is written as a
  platform-neutral sequence of Maven/container commands; the concrete CI definition
  (GitLab CI or GitHub Actions) follows the organization's platform once the remote exists.
- **Alternatives considered**: Jib (no Dockerfile, but less transparent for image
  scanning/hardening reviews); plain Kubernetes manifests (Helm chosen for per-environment
  values such as retention period and cars).

## R12. Scale & performance assumptions

- **Decision**: Designed for one organization: a few hundred to low thousands of users,
  three cars, up to ~500 trips per user (SC-003). Two replicas for availability during
  working hours. Target p95 < 500 ms for page responses; list query uses index
  `(owner_subject, start_at, created_at, id)`.
- **Rationale**: Numbers derive from the spec's success criteria; no pagination needed
  (spec assumption).

---

All Technical Context items are resolved; no `NEEDS CLARIFICATION` remains.
Open business input (not blocking planning): concrete car names/plates, retention period
(DPO), identity provider details (issuer URL, client registration).
