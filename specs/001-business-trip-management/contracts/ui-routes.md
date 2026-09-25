# Contract: UI Routes (HTTP / HTML)

**Feature**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

The application exposes server-rendered HTML pages styled with Bootstrap 5. This contract
defines routes, form fields, responses, and required page content. It is the basis for the
MockMvc integration tests and Playwright E2E tests (Principle II).

## General rules

| Rule | Detail |
|------|--------|
| Authentication | All routes below except `/actuator/health/**` (management port) and static assets require an authenticated OIDC session. Unauthenticated requests → `302` to the IdP login. |
| CSRF | Every `POST` must carry a valid `_csrf` token; otherwise `403`. |
| Pattern | POST-Redirect-GET after successful state changes; validation errors re-render the same step with `200` and the entered values kept. |
| Ownership | `{id}` of a trip not owned by the current user → `404` page "Dienstreise nicht gefunden." (no information disclosure). |
| Read-only | Edit/delete routes for a trip with `start_at ≤ now` → redirect to `/trips` with flash error `trip.readOnly` and no change (FR-023). |
| Wizard state | Carried in form fields (hidden inputs), never trusted: every step re-validates all previous values (FR-008). Every step carries **all** values entered so far (period, car, destination), so "Zurück" never discards input (constitution I). |
| Language / format | `lang="de"`; dates `dd.MM.yyyy`, times `HH:mm`, Europe/Berlin (FR-015). |
| Errors | Error summary at top (`role="alert"`, focused) + field-level `invalid-feedback` linked with `aria-describedby`; page title prefixed with "Fehler: ". |
| Technical failure | Save fails for technical reasons → same step re-rendered with entered data and message "Die Dienstreise konnte nicht gespeichert werden. Bitte versuchen Sie es erneut." (edge case). |

## Form fields

| Name | Type (HTML) | Required | Notes |
|------|-------------|----------|-------|
| `startDate` | `date` (ISO `yyyy-MM-dd` on the wire) | yes | V1, V2 |
| `startTime` | `time` (`HH:mm`) | yes | V1, V2 |
| `endDate` | `date` | yes | V1, V2 |
| `endTime` | `time` | yes | V1, V2 |
| `carId` | `radio` (1–3) | yes (step 2+) | V5, V6 |
| `destination` | `text`, `maxlength="200"` | yes (step 3) | V7, V8; hint "höchstens 200 Zeichen" |
| `version` | `hidden` | edit only | V11 |
| `action` | submit button value | — | `back` or `next`/`save` |

## Routes

### `GET /` 
→ `302 /trips`

### `GET /trips` — List view (US2, FR-011–FR-014)
- `200`. Heading "Meine Dienstreisen". Button "Neue Dienstreise" → `/trips/new`.
- Table (`<table class="table">` with `<caption>` and `<th scope="col">`) with columns:
  Fahrzeug (name + plate), Beginn, Ziel, Ende, Status, Aktionen.
- Only trips of the current user; order `start_at, created_at, id` ascending.
- Actions "Bearbeiten" / "Löschen" (links with accessible names including the trip, e.g.
  "Dienstreise nach Hannover am 01.10.2026 bearbeiten") **only** for upcoming trips.
- Empty state: message "Sie haben noch keine Dienstreisen erfasst." + link "Neue Dienstreise erfassen" (FR-014).
- Flash success message after save/delete (`role="status"`).

### Create wizard (US1, FR-004)

| Step | Route | Behaviour |
|------|-------|-----------|
| 1 Zeitraum | `GET /trips/new` | Form with the four date/time fields. |
| | `POST /trips/new/period` | Validates V1–V4. Error → re-render step 1. OK → render step 2 (car selection) with period in hidden fields. |
| 2 Fahrzeug | (rendered by previous POST) | `fieldset` legend "Fahrzeug wählen"; three radio options showing name + plate; unavailable cars `disabled` with visible "nicht verfügbar". Shows chosen period. If none free → message `trip.car.noneAvailable` and "Zeitraum ändern" button only. |
| | `POST /trips/new/car` | Re-validates V1–V4, validates V5–V6. `action=back` → step 1 with values kept. Error → re-render step 2 with current availability. OK → render step 3. |
| 3 Ziel | (rendered by previous POST) | Shows period and car for review (US1 AS5); field `destination`. |
| | `POST /trips` | Re-validates V1–V8 in one transaction; inserts. `action=back` → step 2. V4 fails → step 1 with values kept. V6 or DB constraint `23P01` → step 2 with refreshed availability and message `trip.car.booked` (US4 AS2). OK → `302 /trips` + flash "Die Dienstreise wurde gespeichert." |

If the user returns to step 1 and changes the period so that the previously chosen car is
no longer free, step 2 is shown with no car selected and the message
"Das zuvor gewählte Fahrzeug ist im neuen Zeitraum nicht verfügbar." (edge case).

### Edit wizard (US3, FR-020)

Same three steps and messages as create, pre-filled from the stored trip; the trip itself is
excluded from the availability check (so its own car is available for its own period).

| Route | Behaviour |
|-------|-----------|
| `GET /trips/{id}/edit` | V9, V10 → step 1 pre-filled. |
| `POST /trips/{id}/edit/period` | as create step 1, plus V9, V10 |
| `POST /trips/{id}/edit/car` | as create step 2, plus V9, V10 |
| `POST /trips/{id}` | as create save, plus V9, V10, V11; updates; `302 /trips` + flash "Die Änderungen wurden gespeichert." |

### Delete (US3, FR-021)

| Route | Behaviour |
|-------|-----------|
| `GET /trips/{id}/delete` | V9, V10 → confirmation page showing car, period, destination; buttons "Endgültig löschen" (POST) and "Abbrechen" (link to `/trips`, no change – US3 AS5). |
| `POST /trips/{id}/delete` | V9, V10 → delete row; `302 /trips` + flash "Die Dienstreise wurde gelöscht." |

### `POST /logout`
Ends the session and performs OIDC RP-initiated logout.

## Operational endpoints (management port, not exposed via ingress)

| Route | Purpose |
|-------|---------|
| `GET /actuator/health/liveness` | Liveness probe |
| `GET /actuator/health/readiness` | Readiness probe (includes DB) |
| `GET /actuator/prometheus` | Metrics (optional, if Prometheus scraping is used besides OTLP) |

## Information disclosure guarantees (FR-019)

No page, message, HTML attribute, or response header rendered for user A contains the
owner, destination, or exact period of a trip belonging to user B. Availability is shown
only as available / "nicht verfügbar" per car for the period user A entered.
