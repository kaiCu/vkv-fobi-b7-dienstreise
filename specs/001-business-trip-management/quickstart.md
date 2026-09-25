# Quickstart & Validation Guide: Business Trip Management

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

This guide shows how to run the application locally and how to check that the feature works
end to end. Routes and messages are defined in [contracts/ui-routes.md](./contracts/ui-routes.md),
configuration in [contracts/configuration.md](./contracts/configuration.md), and validation
rules V1–V11 in [data-model.md](./data-model.md).

## Prerequisites

- JDK 25 (Temurin), Docker / Podman with Compose (for PostgreSQL, Keycloak, Testcontainers)
- A current Firefox or Chromium; optionally the NVDA or Orca screen reader for manual a11y checks

## Setup

```bash
# 1. Start backing services: PostgreSQL 17 + Keycloak with realm "dienstreise"
#    (test users: anna/anna, ben/ben)
docker compose -f compose.dev.yaml up -d

# 2. Run the application with the dev profile (dev supplies cars, retention P90D, OIDC client)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Open http://localhost:8080 → redirected to Keycloak → sign in as anna
```

## Automated validation

```bash
./mvnw verify                 # unit + integration tests (Testcontainers) + ArchUnit
./mvnw verify -Pe2e           # Playwright journeys + axe-core accessibility checks
./mvnw verify -Psecurity      # SAST, dependency scan, SBOM (target/bom.json)
./mvnw verify -Pperf          # list performance (SC-003); dedicated machine, before each release
```

Expected: all green, no skipped tests without a documented reason, no high/critical
security findings, `target/bom.json` present.

## Manual validation scenarios

Use dates in the future relative to today. "Car A/B/C" = the three configured dev cars.

| # | Scenario (spec reference) | Steps | Expected outcome |
|---|---------------------------|-------|------------------|
| 1 | Record a trip (US1) | As anna: "Neue Dienstreise" → start tomorrow 09:00, end tomorrow 12:00 → Weiter → choose car A → destination "Hannover" → Speichern | Success message; list shows the trip with car name + plate, "Hannover", German date format |
| 2 | Validation (US1 AS7–9) | Try: empty end time; end before start; start yesterday; destination of spaces; 201 characters | Trip not stored; error summary focused; each message names the field and the fix |
| 3 | List order & isolation (US2) | As anna create trips on 3 different days in random order; sign in as ben | anna sees her trips earliest first; ben sees only the empty state with the "Neue Dienstreise erfassen" link |
| 4 | Availability (US4 AS1, AS3, AS6) | As ben: period tomorrow 11:00–13:00 | Car A "nicht verfügbar" and not selectable; no hint about anna or "Hannover". Period 12:00–14:00 → car A available |
| 5 | No car free (US1 AS4) | Book cars A, B, C for the same period (anna/ben), then try that period again | Message that no car is free, invitation to change the period |
| 6 | Race condition (US4 AS2) | Two browser windows (anna, ben) reach step 3 with car B for overlapping periods; save in anna's, then ben's | ben returns to the car step, car B now "nicht verfügbar", conflict message |
| 7 | Edit & delete (US3) | As anna edit trip 1 → move to next week, keep car A → save; then delete trip 2 → "Abbrechen", then delete again → "Endgültig löschen" | Edit saved and list re-sorted; cancel keeps trip; delete removes it and its slot becomes available to ben |
| 8 | Read-only & ownership (US3 AS6–7) | Load `dev/seed-past-trip.sql` (see its header comment) to add a past and an ongoing trip for anna; replay a POST to `/trips/{id}/delete` of anna's trip while signed in as ben | No edit/delete buttons for the past trip, manipulated requests refused; ben gets "Dienstreise nicht gefunden." |
| 9 | Keyboard & screen reader (FR-016) | Complete scenario 1 using only the keyboard; repeat with a screen reader | All controls reachable in logical order, visible focus, labels and errors announced, unavailable cars announced as unavailable |
| 10 | Retention (FR-024) | Run with `DIENSTREISE_RETENTION_PERIOD=PT1M` and `DIENSTREISE_RETENTION_CRON="*/30 * * * * *"` (default is hourly), with the past trip from `dev/seed-past-trip.sql` | After the next job run the trip is gone; log entry and metric `dienstreise.retention.deleted` incremented |
| 11 | Operations (Principle III) | `curl localhost:8081/actuator/health/readiness`; stop PostgreSQL | `UP`, then `DOWN`; logs are JSON; app shuts down gracefully on SIGTERM |
| 12 | Startup guards | Start with `prod` profile without `DIENSTREISE_RETENTION_PERIOD`, or with only two cars | Startup refused with a clear error message |

## Done criteria

All automated suites green, all 12 scenarios pass, the accessibility audit (SC-006) has
no critical findings, and the usability test (`docs/usability-test.md`, SC-001, SC-002, SC-008)
meets its targets. CI/CD is deferred (plan.md Complexity Tracking), so run the `ci/*.sh`
scripts locally before merging.
