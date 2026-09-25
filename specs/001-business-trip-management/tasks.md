---

description: "Task list for Business Trip Management"
---

# Tasks: Business Trip Management

**Input**: Design documents from `/specs/001-business-trip-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-routes.md, contracts/configuration.md, quickstart.md

**Tests**: REQUIRED. Constitution Principle II (TDD, non-negotiable) says acceptance criteria must become automated tests before implementation. In every phase, write the test tasks first, run them, and confirm they **fail** before starting the implementation tasks.

**Organization**: Tasks are grouped by user story. Order follows spec priority: US1 (P1), US4 (P1), US2 (P2), US3 (P3). Retention (FR-024) and operations are cross-cutting phases.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: User story from spec.md (US1–US4)

## Path Conventions

Single project at repository root (see plan.md):

- Main code: `src/main/java/de/lgln/dienstreise/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/de/lgln/dienstreise/{unit,integration,e2e,architecture,support}/`
- Test naming: `*Test` = unit (Surefire), `*IT` = integration (Failsafe, Testcontainers), `*E2eTest` = browser tests (Failsafe, profile `e2e` only)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create `pom.xml` with parent `spring-boot-starter-parent` 4.0 (latest stable patch release), `java.version` 25, and dependencies: `spring-boot-starter-web`, `-thymeleaf`, `-security`, `-oauth2-client`, `-data-jpa`, `-validation`, `-actuator`, `spring-session-jdbc`, `flyway-core`, `flyway-database-postgresql`, `postgresql` (runtime), `org.webjars.npm:bootstrap` 5.3.x, `webjars-locator-lite`, `micrometer-registry-otlp`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`; test scope: `spring-boot-starter-test`, `spring-security-test`, `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`, `com.github.dasniko:testcontainers-keycloak`, `com.microsoft.playwright:playwright`, `com.deque.html.axe-core:playwright`, `com.tngtech.archunit:archunit-junit5`. Configure Surefire (`**/*Test.java`, excluding `**/*E2eTest.java`) and Failsafe (`**/*IT.java`), and add a Maven profile `e2e` in which Failsafe also runs `**/*E2eTest.java`, and a profile `perf` in which Failsafe runs only `**/*PerfIT.java` (excluded from the default build). **Pinning (constitution IV)**: before writing the file, look up the current stable patch release of every dependency and plugin not managed by the Spring Boot BOM (Maven Central), and write exact versions as `<properties>`. No ranges, no `x` placeholders, no `LATEST`. No Thymeleaf Spring Security extras dependency: the user name comes from `CurrentUserModelAdvice` (T037), and CSRF fields are inserted automatically into `th:action` forms
- [X] T002 Add the Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, Maven 3.9.x) at the repository root
- [X] T003 Add Spotless (google-java-format) and Maven Enforcer (require Java 25, Maven 3.9, `dependencyConvergence`, `banDuplicatePomDependencyVersions`) to the `<build><plugins>` section of `pom.xml`
- [X] T004 [P] Create `.gitignore` (target/, .idea/, *.iml, .vscode/, .env, playwright traces) and `.editorconfig` (UTF-8, LF, 4 spaces Java, 2 spaces YAML/HTML) at the repository root
- [X] T005 [P] Create `compose.dev.yaml` with services `postgres` (image `postgres:17.<latest patch>`, exact tag, db/user/password `dienstreise`, port 5432, named volume) and `keycloak` (image `quay.io/keycloak/keycloak:<latest stable 26 release>`, exact tag, same tag as in T044, `start-dev --import-realm`, port 8180, mounts `dev/keycloak/realm-dienstreise.json` to `/opt/keycloak/data/import/`)
- [X] T006 [P] Create `dev/keycloak/realm-dienstreise.json`: realm `dienstreise`, confidential OIDC client `dienstreise` (secret `dev-secret`, standard flow only, redirect URIs `http://localhost:*/login/oauth2/code/*`, post-logout redirect `http://localhost:*/*`), users `anna`/`anna` (name "Anna Beispiel", fixed `"id": "00000000-0000-0000-0000-00000000a001"`) and `ben`/`ben` (name "Ben Muster", fixed `"id": "00000000-0000-0000-0000-00000000b001"`), both with e-mail verified and no required actions. Fixed ids keep `owner_subject` stable for dev seed data (T007)
- [X] T007 [P] Create `dev/seed-past-trip.sql` (dev only, never loaded by Flyway), which inserts for anna (`owner_subject` = `'http://localhost:8180/realms/dienstreise|00000000-0000-0000-0000-00000000a001'`) one past trip (car 1, `now() - interval '3 days'` to `now() - interval '3 days' + interval '4 hours'`) and one ongoing trip (car 2, `now() - interval '1 hour'` to `now() + interval '2 hours'`), with destinations "Oldenburg" and "Braunschweig". Document in a header comment: `docker compose -f compose.dev.yaml exec -T postgres psql -U dienstreise dienstreise < dev/seed-past-trip.sql` (after the app has started once, so the `car` rows exist)
- [X] T008 Create `src/main/java/de/lgln/dienstreise/DienstreiseApplication.java` (`@SpringBootApplication`, `@EnableScheduling`, `@ConfigurationPropertiesScan`) and an empty smoke test `src/test/java/de/lgln/dienstreise/integration/ApplicationContextIT.java` (initially `@Disabled("enabled in T018")`; enabled once test support exists)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, security, configuration, layout, shared domain types, and test infrastructure used by all stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Test infrastructure

- [X] T009 Create `src/test/java/de/lgln/dienstreise/support/MutableClock.java`, a `java.time.Clock` subclass with `setInstant(Instant)` and `advance(Duration)`, zone UTC
- [X] T010 Create `src/test/java/de/lgln/dienstreise/support/TestcontainersConfig.java` (`@TestConfiguration(proxyBeanMethods = false)`, `PostgreSQLContainer<?>` bean for image `postgres:17` with `@ServiceConnection`, reused across tests) and `src/test/java/de/lgln/dienstreise/support/TestClockConfig.java` (`@TestConfiguration` with `@Primary MutableClock` bean, default `2026-10-01T06:00:00Z`)
- [X] T011 Create `src/test/java/de/lgln/dienstreise/support/AbstractIntegrationTest.java`: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Import({TestcontainersConfig.class, TestClockConfig.class})`, `@ActiveProfiles("test")`. Provide helpers `anna()` / `ben()` returning `SecurityMockMvcRequestPostProcessors.oidcLogin()` with issuer `https://idp.test/realms/dienstreise`, `sub` `anna-sub` / `ben-sub`, `name` claims, and a `@BeforeEach` that deletes all rows from `business_trip` and resets the clock
- [X] T012 Create `src/test/resources/application-test.yaml` with test cars given under the **environment-variable names** as top-level keys (`DIENSTREISE_CARS_0_NAME: Dienstwagen 1`, `DIENSTREISE_CARS_0_LICENSE_PLATE: H-LG 101`, … `Dienstwagen 3`/`H-LG 103`), not as `dienstreise.cars[..]`, so every test exercises the placeholders of `application.yaml` (T027), `dienstreise.retention.period: P90D`, a dummy OIDC client registration (issuer `https://idp.test/realms/dienstreise`, explicit `authorization-uri`/`token-uri`/`jwk-set-uri` so no discovery is needed), and `dienstreise.retention.cron: "-"` (disabled)

### Tests for foundational components (write first, must fail)

- [X] T013 [P] Write `src/test/java/de/lgln/dienstreise/integration/FlywayMigrationIT.java` (extends AbstractIntegrationTest, uses `JdbcTemplate`). Assert: tables `car`, `business_trip`, `spring_session` exist. Inserting `end_at = start_at` fails with SQLState `23514`. Inserting destination `'   '` fails `23514`. Inserting car id `4` fails `23514`. Two trips for car 1 with `[09:00,12:00)` and `[11:00,13:00)` fail with SQLState `23P01`. `[09:00,12:00)` and `[12:00,14:00)` for car 1 both succeed. Overlapping trips on different cars succeed
- [X] T014 [P] Write `src/test/java/de/lgln/dienstreise/unit/DienstreisePropertiesTest.java` (Spring `ApplicationContextRunner` + `@EnableConfigurationProperties`). Startup fails when fewer or more than 3 cars are configured, a name is blank, a name exceeds 80 characters, a plate is blank, exceeds 15 characters, or is duplicated, or `retention.period` is zero or negative. It succeeds with valid values
- [X] T015 [P] Write `src/test/java/de/lgln/dienstreise/unit/TripPeriodTest.java`. Test `TripPeriod.ofLocal(startDate, startTime, endDate, endTime)` in Europe/Berlin: summer time `2026-07-01 09:00` → `07:00Z`. The ambiguous `2026-10-25 02:30` resolves to the earlier offset (+02:00 → `00:30Z`). The non-existent `2027-03-28 02:30` shifts forward to `03:30` local. `overlaps()` uses half-open semantics: `[9,12)` vs `[11,13)` true, `[9,12)` vs `[12,14)` false, identical true, contained true
- [X] T016 [P] Write `src/test/java/de/lgln/dienstreise/unit/TripStatusTest.java`. `TripStatus.of(start, end, now)` returns `UPCOMING` when `now < start`, `ONGOING` when `start ≤ now < end`, and `PAST` when `now ≥ end`. `now == start` returns `ONGOING` (read-only per FR-023)
- [X] T017 [P] Write `src/test/java/de/lgln/dienstreise/unit/GermanDateTimeFormatTest.java`. `GermanDateTimeFormat.dateTime(Instant.parse("2026-09-27T12:30:00Z"))` returns `"27.09.2026, 14:30"`, `date(...)` returns `"27.09.2026"`, and `time(...)` returns `"14:30"` (Europe/Berlin, FR-015). A winter date uses +01:00
- [X] T018 [P] Write `src/test/java/de/lgln/dienstreise/integration/SecurityConfigIT.java` and remove `@Disabled` from `ApplicationContextIT`. `GET /trips` unauthenticated returns 302 to `/oauth2/authorization/dienstreise`. `GET /webjars/bootstrap/dist/css/bootstrap.min.css` unauthenticated returns 200. `POST /logout` without CSRF returns 403. Every response carries `Content-Security-Policy` containing `default-src 'self'`, `frame-ancestors 'none'` and `form-action 'self'`, plus `X-Content-Type-Options: nosniff` and `Referrer-Policy: same-origin`
- [X] T019 [P] Write `src/test/java/de/lgln/dienstreise/integration/SessionSecurityIT.java` (`webEnvironment = RANDOM_PORT`, `TestRestTemplate` or `java.net.http.HttpClient` without redirects, property `DIENSTREISE_SESSION_TIMEOUT=17m`). `GET /trips` unauthenticated creates a session; its `Set-Cookie` for `SESSION` contains `Secure`, `HttpOnly` and `SameSite=Lax`, and never `Domain=`. After that request, `spring_session` contains a row, which proves sessions live in the database and pods stay stateless. Its `max_inactive_interval` equals `1020` seconds, which proves the configured idle timeout without waiting. Spring Session's default cookie name `SESSION` is expected (no `JSESSIONID` cookie at all)
- [X] T020 [P] Write `src/test/java/de/lgln/dienstreise/integration/CarCatalogInitializerIT.java`. After startup the `car` table contains ids 1–3 with the names and plates from `application-test.yaml`. Changing the configured name for index 0 and calling `CarCatalogInitializer.run()` again updates row 1 without changing its id. Add a `@Nested` test whose `ApplicationContextInitializer` adds a `SystemEnvironmentPropertySource` (highest precedence) with `DIENSTREISE_CARS_2_LICENSE_PLATE=H-LG 999`; assert that car 3 then has plate `H-LG 999`. This proves the real environment variables bind (automatic mapping would turn `LICENSE_PLATE` into `license.plate` and silently not bind)
- [X] T021 [P] Write `src/test/java/de/lgln/dienstreise/integration/ActuatorIT.java` (`webEnvironment = RANDOM_PORT`, random management port). `GET /actuator/health/liveness` and `/actuator/health/readiness` on the management port return 200 with `{"status":"UP"}` without authentication. `/actuator/env` is not exposed (404)
- [X] T022 [P] Write `src/test/java/de/lgln/dienstreise/architecture/ArchitectureTest.java` (ArchUnit). No class outside `..config..` calls `Instant.now()`, `LocalDateTime.now()`, `LocalDate.now()`, `ZonedDateTime.now()` or `Clock.system*`. Classes in `..web..` do not depend on classes named `*Repository`. No class uses `System.out` or `System.err`
- [X] T023 [P] Write `src/test/java/de/lgln/dienstreise/unit/TemplateSafetyTest.java`, which scans all files under `src/main/resources/templates/`. It fails if any contains `th:utext`, an inline `<script>` without `src`, a `style="` attribute, or an `http://`/`https://` URL in a `src`, `href` or `action` attribute (no CDN, strict CSP). Namespace declarations (`xmlns:th="http://www.thymeleaf.org"` and other `xmlns:*` attributes) are allowed. The `lang="de"` requirement is not checked here but on rendered pages (T049, T071)

### Implementation (make the tests above pass)

- [X] T024 [P] Create `src/main/resources/db/migration/V1__car.sql`: table `car` with `id smallint PRIMARY KEY CHECK (id BETWEEN 1 AND 3)`, `display_name varchar(80) NOT NULL CHECK (btrim(display_name) <> '')`, `license_plate varchar(15) NOT NULL UNIQUE CHECK (btrim(license_plate) <> '')`
- [X] T025 [P] Create `src/main/resources/db/migration/V2__business_trip.sql`:
  - `CREATE EXTENSION IF NOT EXISTS btree_gist`
  - Table `business_trip` with columns:
    - `id uuid PRIMARY KEY`
    - `owner_subject varchar(255) NOT NULL`
    - `car_id smallint NOT NULL REFERENCES car(id) ON DELETE RESTRICT`
    - `start_at timestamptz NOT NULL`
    - `end_at timestamptz NOT NULL`
    - `destination varchar(200) NOT NULL`
    - `created_at timestamptz NOT NULL DEFAULT now()`
    - `updated_at timestamptz NOT NULL`
    - `version integer NOT NULL DEFAULT 0`
  - `CONSTRAINT business_trip_end_after_start CHECK (end_at > start_at)`
  - `CONSTRAINT business_trip_destination_len CHECK (char_length(btrim(destination)) BETWEEN 1 AND 200)`
  - `CONSTRAINT business_trip_no_overlap EXCLUDE USING gist (car_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)`
  - `CREATE INDEX business_trip_owner_start_idx ON business_trip (owner_subject, start_at, created_at, id)`
  - `CREATE INDEX business_trip_end_idx ON business_trip (end_at)`
- [X] T026 [P] Create `src/main/resources/db/migration/V3__spring_session.sql` with the Spring Session JDBC PostgreSQL schema (`spring_session`, `spring_session_attributes`, indexes), copied from `org/springframework/session/jdbc/schema-postgresql.sql` of the pinned Spring Session version
- [X] T027 Create `src/main/resources/application.yaml`:
  - `spring.application.name: dienstreise`
  - `spring.jpa.open-in-view: false`, `spring.jpa.hibernate.ddl-auto: validate`
  - `spring.flyway.enabled: true`
  - `spring.session.jdbc.initialize-schema: never`, `server.servlet.session.timeout: ${DIENSTREISE_SESSION_TIMEOUT:30m}`, `server.servlet.session.cookie.{secure: true, http-only: true, same-site: lax}`
  - `spring.messages.basename: messages`, `spring.messages.fallback-to-system-locale: false`, `spring.web.locale: de_DE`, `spring.web.locale-resolver: fixed`
  - `server.shutdown: graceful`, `server.error.include-stacktrace: never`, `server.error.whitelabel.enabled: false`
  - `management.server.port: ${MANAGEMENT_SERVER_PORT:8081}`, `management.endpoints.web.exposure.include: health,prometheus`, `management.endpoint.health.probes.enabled: true`, `management.health.readinessstate.enabled: true`
  - `logging.structured.format.console: ecs`
  - `management.otlp.tracing.endpoint` and `management.otlp.metrics.export.url` from `OTEL_EXPORTER_OTLP_ENDPOINT` (export disabled when unset)
  - OIDC client registration `dienstreise` (`issuer-uri: ${DIENSTREISE_OIDC_ISSUER_URI}`, `client-id`, `client-secret`, `scope: openid,profile`)
  - `dienstreise.cars` as a YAML list of three entries with **explicit placeholders** (`- name: ${DIENSTREISE_CARS_0_NAME}` / `license-plate: ${DIENSTREISE_CARS_0_LICENSE_PLATE}`, indices 0–2). Do not rely on Spring's automatic environment-variable mapping, which does not map `_LICENSE_PLATE` to `licensePlate`
  - `dienstreise.retention.period: ${DIENSTREISE_RETENTION_PERIOD:}`, `dienstreise.retention.cron: ${DIENSTREISE_RETENTION_CRON:0 0 * * * *}`

  Also create `application-dev.yaml` (datasource `jdbc:postgresql://localhost:5432/dienstreise`, Keycloak issuer `http://localhost:8180/realms/dienstreise`, client secret `dev-secret`, cars as top-level keys `DIENSTREISE_CARS_<n>_NAME` / `DIENSTREISE_CARS_<n>_LICENSE_PLATE` with `VW ID.3 (blau)`/`H-LG 1001`, `Škoda Enyaq (weiß)`/`H-LG 1002`, `Opel Corsa-e (grau)`/`H-LG 1003`, retention `P90D`, `server.servlet.session.cookie.secure: false`) and `application-prod.yaml` (no defaults for secrets, cars or retention), all in `src/main/resources/`
- [X] T028 Create `src/main/java/de/lgln/dienstreise/config/DienstreiseProperties.java`, a `@ConfigurationProperties("dienstreise") @Validated` record with `@Size(min = 3, max = 3) @Valid List<CarProperties> cars` (record `CarProperties(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 15) String licensePlate)`), plus a custom check that plates are unique. Add `Retention(Duration period, String cron)`, where `period` must be positive when set. In profile `prod`, a missing `period` fails startup with the message "DIENSTREISE_RETENTION_PERIOD ist nicht gesetzt – Aufbewahrungsfrist mit dem Datenschutzbeauftragten festlegen (FR-024)." (makes T014 pass)
- [X] T029 [P] Create `src/main/java/de/lgln/dienstreise/config/ClockConfig.java` with a `@Bean Clock clock()` returning `Clock.systemUTC()`, and set `-Duser.timezone=UTC` in the Spring Boot Maven plugin `jvmArguments` in `pom.xml`
- [X] T030 [P] Create `src/main/java/de/lgln/dienstreise/trip/domain/TripPeriod.java`: record `(Instant start, Instant end)` with constant `ZONE = ZoneId.of("Europe/Berlin")`. Add factory `ofLocal(LocalDate, LocalTime, LocalDate, LocalTime)` using `ZonedDateTime.of(...)` (earlier offset on overlap, shift forward in gap), `overlaps(TripPeriod other)` as `start < other.end && other.start < end`, and `isEndAfterStart()` (makes T015 pass)
- [X] T031 [P] Create `src/main/java/de/lgln/dienstreise/trip/domain/TripStatus.java`, an enum `UPCOMING`, `ONGOING`, `PAST` with message keys `trip.status.upcoming` ("Geplant"), `trip.status.ongoing` ("Läuft") and `trip.status.past` ("Abgeschlossen"), and `static TripStatus of(Instant start, Instant end, Instant now)` (makes T016 pass)
- [X] T032 [P] Create `src/main/java/de/lgln/dienstreise/web/GermanDateTimeFormat.java`, a `@Component("fmt")` with pattern `dd.MM.yyyy, HH:mm`, `Locale.GERMANY`, zone Europe/Berlin, and methods `dateTime(Instant)`, `date(Instant)`, `time(Instant)` usable in Thymeleaf as `${@fmt.dateTime(...)}` (makes T017 pass)
- [X] T033 [P] Create `src/main/java/de/lgln/dienstreise/car/Car.java` (JPA entity mapped to `car`: `Short id`, `String displayName`, `String licensePlate`, method `label()` returning `displayName + " (" + licensePlate + ")"`) and `src/main/java/de/lgln/dienstreise/car/CarRepository.java` (`JpaRepository<Car, Short>`, `List<Car> findAllByOrderByIdAsc()`)
- [X] T034 Create `src/main/java/de/lgln/dienstreise/car/CarCatalogInitializer.java`, an `ApplicationRunner` that is `@Transactional` and upserts ids 1–3 from `DienstreiseProperties.cars()` (index i → id i+1). It never deletes rows and logs `car catalog reconciled` with ids only (makes T020 pass; depends on T028, T033)
- [X] T035 [P] Create `src/main/java/de/lgln/dienstreise/trip/domain/BusinessTrip.java`, a JPA entity mapped to `business_trip` with fields `UUID id`, `String ownerSubject`, `@ManyToOne(fetch = LAZY) Car car`, `Instant startAt`, `Instant endAt`, `String destination`, `Instant createdAt`, `Instant updatedAt`, and `@Version int version`. Add a constructor `BusinessTrip(UUID id, String ownerSubject, Car car, TripPeriod period, String destination, Instant now)`, a method `change(Car car, TripPeriod period, String destination, Instant now)`, `TripPeriod period()`, and `TripStatus status(Instant now)`. There are no setters
- [X] T036 Create `src/main/java/de/lgln/dienstreise/trip/TripRepository.java` (`JpaRepository<BusinessTrip, UUID>`) with owner-scoped methods only: `Optional<BusinessTrip> findByIdAndOwnerSubject(UUID id, String ownerSubject)` and `List<BusinessTrip> findByOwnerSubjectOrderByStartAtAscCreatedAtAscIdAsc(String ownerSubject)` (with `@EntityGraph(attributePaths = "car")`). Add two JPQL queries, not one with a nullable parameter: a null `UUID` parameter in `:x is null` fails on PostgreSQL with "could not determine data type of parameter". The queries are `boolean existsOverlap(Short carId, Instant start, Instant end)`: `select count(t) > 0 from BusinessTrip t where t.car.id = :carId and t.startAt < :end and :start < t.endAt`, and `boolean existsOverlapExcluding(Short carId, Instant start, Instant end, UUID excludeId)`, the same query with `and t.id <> :excludeId`. Add the `@Modifying` query `int deleteByEndAtBefore(Instant cutoff)`
- [X] T037 Create `src/main/java/de/lgln/dienstreise/web/CurrentUser.java` (record `subject`, `displayName`; `subject` = `oidcUser.getIssuer() + "|" + oidcUser.getSubject()`, `displayName` = `name` claim, falling back to `preferred_username`) and `src/main/java/de/lgln/dienstreise/web/CurrentUserArgumentResolver.java`, a `HandlerMethodArgumentResolver` registered in `src/main/java/de/lgln/dienstreise/config/WebConfig.java`. Add `@ControllerAdvice` `src/main/java/de/lgln/dienstreise/web/CurrentUserModelAdvice.java`, which puts `currentUserName` into every model. The display name is never persisted
- [X] T038 Create `src/main/java/de/lgln/dienstreise/config/SecurityConfig.java` (`SecurityFilterChain`):
  - `permitAll` for `/webjars/**`, `/css/**`, `/favicon.ico` and `/error`; everything else `authenticated()`
  - `oauth2Login()` with the default `/oauth2/authorization/dienstreise` login page
  - CSRF enabled with the default session repository
  - Session fixation `changeSessionId`
  - Headers: CSP `default-src 'self'; img-src 'self' data:; frame-ancestors 'none'; form-action 'self'; base-uri 'self'`, HSTS (1 year, includeSubDomains), `Referrer-Policy: same-origin`, frame options DENY
  - Logout via `POST /logout` using `OidcClientInitiatedLogoutSuccessHandler` with post-logout redirect `{baseUrl}/`

  (makes T018 pass; depends on T027)
- [X] T039 Create `src/main/resources/messages.properties` (UTF-8) with all texts. Validation messages, verbatim from data-model.md:
  - `trip.start.required=Bitte geben Sie das Startdatum an.`
  - `trip.startTime.required=Bitte geben Sie die Startzeit an.`
  - `trip.end.required=Bitte geben Sie das Enddatum an.`
  - `trip.endTime.required=Bitte geben Sie die Endzeit an.`
  - `trip.date.invalid=Bitte geben Sie ein gültiges Datum an.`
  - `trip.time.invalid=Bitte geben Sie eine gültige Uhrzeit an.`

    (No format in the text: native date and time inputs display a browser-specific format and send ISO values.)
  - `trip.end.notAfterStart=Das Ende muss nach dem Beginn liegen.`
  - `trip.start.notInFuture=Dienstreisen können nur mit einem Beginn in der Zukunft gebucht werden. Bitte wählen Sie einen späteren Beginn.`
  - `trip.car.required=Bitte wählen Sie ein Fahrzeug aus.`
  - `trip.car.booked=Dieses Fahrzeug ist im gewählten Zeitraum bereits gebucht. Bitte wählen Sie ein anderes Fahrzeug oder einen anderen Zeitraum.`
  - `trip.car.noneAvailable=Für diesen Zeitraum ist kein Fahrzeug frei. Bitte ändern Sie Beginn oder Ende.`
  - `trip.car.noLongerAvailable=Das zuvor gewählte Fahrzeug ist im neuen Zeitraum nicht verfügbar.`
  - `trip.destination.required=Bitte geben Sie das Reiseziel an.`
  - `trip.destination.tooLong=Das Reiseziel darf höchstens 200 Zeichen lang sein.`
  - `trip.destination.invalidChars=Das Reiseziel enthält unzulässige Steuerzeichen.`
  - `trip.readOnly=Diese Dienstreise hat bereits begonnen und kann nicht mehr geändert oder gelöscht werden.`
  - `trip.staleEdit=Die Dienstreise wurde zwischenzeitlich geändert. Bitte laden Sie sie neu.`
  - `trip.notFound=Dienstreise nicht gefunden.`
  - `trip.saveFailed=Die Dienstreise konnte nicht gespeichert werden. Bitte versuchen Sie es erneut.`

  Status labels: `trip.status.upcoming=Geplant`, `trip.status.ongoing=Läuft`, `trip.status.past=Abgeschlossen`.

  Flash messages:
  - `flash.created=Die Dienstreise wurde gespeichert.`
  - `flash.updated=Die Änderungen wurden gespeichert.`
  - `flash.deleted=Die Dienstreise wurde gelöscht.`

  UI labels:
  - `label.startDate=Startdatum`, `label.startTime=Startzeit`, `label.endDate=Enddatum`, `label.endTime=Endzeit`
  - `label.car=Fahrzeug`, `label.destination=Reiseziel`
  - `label.required=Pflichtfeld`, `label.unavailable=nicht verfügbar`, `hint.destination=höchstens 200 Zeichen`
  - `button.next=Weiter`, `button.back=Zurück`, `button.save=Speichern`, `button.cancel=Abbrechen`, `button.deleteConfirm=Endgültig löschen`
  - `nav.skip=Zum Inhalt springen`, `nav.logout=Abmelden`, `app.title=Dienstreise`
  - `errorSummary.title=Bitte korrigieren Sie folgende Angaben:`, `page.errorPrefix=Fehler: `
- [X] T040 Create `src/main/resources/templates/layout.html`, a Thymeleaf layout using fragment parameters `title` and `content`:
  - `<html lang="de">`, `<meta name="viewport" content="width=device-width, initial-scale=1">`
  - `<title>` = optional `page.errorPrefix` + page title + " – Dienstreise"
  - `<link>` to `/webjars/bootstrap/dist/css/bootstrap.min.css` and `/css/app.css`; `<script defer src="/webjars/bootstrap/dist/js/bootstrap.bundle.min.js">`
  - Skip link `<a class="visually-hidden-focusable" href="#main">` (`nav.skip`)
  - `<header>` with `navbar` showing the app name linked to `/trips`, the `currentUserName`, and a logout `<form method="post" action="/logout">` with CSRF and button `nav.logout`
  - `<main id="main" class="container py-4" tabindex="-1">` and a `<footer>`

  Also create `src/main/resources/static/css/app.css` with a visible focus outline for `:focus-visible` (`outline: 3px solid #0a58ca; outline-offset: 2px`), a minimum target size of 44px for `.btn` and `.form-check-input`, and no colour-only status indication
- [X] T041 [P] Create fragments in `src/main/resources/templates/fragments/`:
  - `error-summary.html`: renders when `#fields.hasErrors('*')` or a `globalError` model attribute is present. It is a `<div class="alert alert-danger" role="alert" tabindex="-1" id="error-summary">` with heading `errorSummary.title` and a `<ul>` of links `href="#<field>"` to each field error
  - `flash.html`: renders the `flashSuccess` attribute as `alert alert-success` with `role="status"`, and `flashError` as `alert alert-danger` with `role="alert"`
  - `form-field.html`: parameterized fragment for `date`/`time`/`text` inputs. It renders a visible `<label for>` with the text "(Pflichtfeld)", `class="form-control"`, `is-invalid`/`aria-invalid="true"` on error, `aria-describedby` pointing to the hint id and the `<div class="invalid-feedback" id="<field>-error">`, and the `required` attribute
  - `trip-summary.html`: `<dl>` with car label, Beginn, Ende and optionally Ziel, formatted via `@fmt`
- [X] T042 [P] Create `src/main/resources/static/js/error-summary.js` (external file, CSP-safe), which focuses `#error-summary` on page load when present. Include it with `defer` in `layout.html`
- [X] T043 Create `src/main/java/de/lgln/dienstreise/trip/domain/TripNotFoundException.java` (in the domain package, so the service layer never depends on `web`) and `src/main/java/de/lgln/dienstreise/web/GlobalExceptionHandler.java` (`@ControllerAdvice`). Map `TripNotFoundException` to HTTP 404 with view `error/404`, and any unexpected exception to 500 with view `error/500` and a log entry at ERROR with a correlation id, never exposing the exception message. Create `src/main/resources/templates/error/404.html` (text `trip.notFound`, link back to `/trips`) and `src/main/resources/templates/error/500.html` (generic text and correlation id). Both use `layout.html`, so they get skip link, landmarks and `lang="de"`
- [X] T044 Create the E2E base `src/test/java/de/lgln/dienstreise/e2e/support/E2eTestBase.java`:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` with the Testcontainers PostgreSQL and a static `KeycloakContainer` (image `quay.io/keycloak/keycloak:26.x`, `withRealmImportFile("keycloak/realm-dienstreise.json")`)
  - `@DynamicPropertySource` sets the issuer URI and client secret
  - Playwright `Browser` (Chromium, headless) per class, a new `BrowserContext` per test with `locale("de-DE")` and `timezoneId("Europe/Berlin")`
  - Helpers: `loginAs(String user)` (fills Keycloak login form), `assertNoAccessibilityViolations(Page page)` using `AxeBuilder(page).withTags(List.of("wcag2a","wcag2aa","wcag21a","wcag21aa"))` and asserting `violations` is empty with a readable report, and `tripsBaseUrl()`

  Copy `dev/keycloak/realm-dienstreise.json` to `src/test/resources/keycloak/realm-dienstreise.json`, with redirect URIs `*` for test only
- [X] T045 Write `src/test/java/de/lgln/dienstreise/e2e/SessionSecurityE2eTest.java` (real Keycloak login). The `SESSION` cookie value before login (after the redirect to the IdP) differs from the value after returning from the IdP (session fixation protection). After `POST /logout`, the old cookie value no longer grants access: a request to `/trips` with it redirects to login, and its `spring_session` row is deleted

**Checkpoint**: `./mvnw verify` and `./mvnw verify -Pe2e -Dit.test=SessionSecurityE2eTest` green. Schema, config validation, security, layout and test infrastructure are in place, so user stories can start.

---

## Phase 3: User Story 1 - Record a business trip with a pool car (Priority: P1) 🎯 MVP

**Goal**: A signed-in employee enters the period, sees the three cars with their availability, chooses a free car, enters the destination, saves, and gets a confirmation (FR-001–FR-010, FR-004a, FR-015, FR-016).

**Independent Test**: Sign in as anna, enter a future period, select a free car, enter "Hannover", and save. A success message is shown, and after reloading the stored trip is in the database with owner `anna`. Each validation error (US1 AS7–AS9) blocks saving with the specified message.

### Tests for User Story 1 (write first, must fail) ⚠️

- [X] T046 [P] [US1] Write `src/test/java/de/lgln/dienstreise/unit/TripFormValidatorTest.java` for `TripFormValidator`, with the clock fixed at `2026-10-01T06:00:00Z` (08:00 Berlin). Error codes per field:
  - Each of `startDate`, `startTime`, `endDate`, `endTime` missing → `trip.start.required`, `trip.startTime.required`, `trip.end.required`, `trip.endTime.required` (V1)
  - End equal to or before start → `trip.end.notAfterStart` on `endTime` (V3)
  - Start at `2026-10-01 08:00` (== now) or earlier → `trip.start.notInFuture` on `startDate` (V4)
  - Period step: `carId` null → `trip.car.required` (V5); `carId` 4 → `trip.car.required`
  - Destination step:
    - `"   "` → `trip.destination.required` (V7)
    - 201 characters → `trip.destination.tooLong` (V8); exactly 200 characters → valid
    - A string containing `\u0007` → `trip.destination.invalidChars`
    - `"  Hannover  "` → valid and normalized to `"Hannover"`
- [X] T047 [P] [US1] Write `src/test/java/de/lgln/dienstreise/integration/AvailabilityServiceIT.java`. With a trip on car 1 for `2026-10-02 09:00–12:00` Berlin: `availabilityFor(10:00–11:00, null)` returns car 1 unavailable and cars 2 and 3 available, in id order with labels. `availabilityFor(12:00–14:00, null)` returns all available. `availabilityFor(10:00–11:00, <that trip id>)` returns car 1 available. With all three cars booked, `noneAvailable()` is true
- [X] T048 [P] [US1] Write `src/test/java/de/lgln/dienstreise/integration/TripServiceCreateIT.java`. `create(anna, validCommand)` stores a row with the `owner_subject` of anna, trimmed destination, and `created_at` = clock instant. Re-validation at save: advancing the clock past the start before `create` throws a `TripValidationException` with code `trip.start.notInFuture` and stores nothing (V4 at save). Creating for a car that is booked for an overlapping period throws `CarAlreadyBookedException` and stores nothing (V6 check before insert)
- [X] T049 [P] [US1] Write `src/test/java/de/lgln/dienstreise/integration/TripWizardCreateIT.java` (MockMvc, `with(anna())`, `with(csrf())`):
  1. `GET /trips/new` returns 200 with `<html lang="de">`, inputs `startDate`, `startTime`, `endDate`, `endTime` (types `date`/`time`, `required`) and labels (AS1).
  2. `POST /trips/new/period` with a valid future period returns 200 and the car step: legend "Fahrzeug wählen", three radio inputs `carId` with labels containing name and plate, and hidden period fields (AS2).
  3. With car 1 booked in that period, car 1's radio has `disabled` and its label contains "nicht verfügbar" (AS3).
  4. With all cars booked, the response shows `trip.car.noneAvailable` and no enabled radio (AS4).
  5. `POST /trips/new/car` with `carId=2` returns 200 and the destination step, showing the period in German format and the car label for review (AS5).
  6. `POST /trips` with valid data returns 302 to `/trips`; the flash `flash.created` is shown on `/trips`; one row exists (AS6).
  7. Missing fields re-render 200 with `#error-summary` listing each error, `aria-invalid="true"` on the fields, entered values kept, and `<title>` starting with "Fehler: " (AS7).
  8. End before start → `trip.end.notAfterStart` (AS8).
  9. Start in the past → `trip.start.notInFuture` at the period step and at `POST /trips` after the clock advances, with entered data kept (AS9, edge case).
  10. `POST /trips/new/car` with `carId` of an unavailable car (manipulated) is rejected with `trip.car.booked`.
  11. `action=back` on the car step returns step 1 with values kept.
  12. After going back and changing the period so the chosen car is booked, the car step shows no car selected and `trip.car.noLongerAvailable`. If the chosen car is still free, it stays pre-selected.
  13. When `TripService.create` throws `DataAccessResourceFailureException` (via `@MockitoSpyBean`), the destination step is re-rendered with `trip.saveFailed` and the data kept.
  14. No data loss (constitution I): fill steps 1–3 (destination "Hannover"), click "Zurück" twice to step 1, then "Weiter" twice without changing anything. Step 2 has the same car pre-selected, step 3 has "Hannover" pre-filled; assert the hidden inputs `carId` and `destination` are present on steps 1 and 2.
- [X] T050 [P] [US1] Write `src/test/java/de/lgln/dienstreise/e2e/RecordTripE2eTest.java` (Playwright). As anna, complete the journey **using only the keyboard** (Tab/Space/Enter): "Neue Dienstreise" → period tomorrow 09:00–12:00 → Weiter → select car 1 → Weiter → "Hannover" → Speichern. Assert the success message is visible and `role=status`, and that it is still visible in the DB-backed list after reload. On each of the three wizard steps, and on the error state of step 1, call `assertNoAccessibilityViolations`. Assert that the error summary receives focus after an invalid submit. Open `/trips/00000000-0000-0000-0000-000000000000/edit` and run `assertNoAccessibilityViolations` on the 404 page

### Implementation for User Story 1

- [X] T051 [P] [US1] Create `src/main/java/de/lgln/dienstreise/trip/web/TripForm.java`, a mutable form-backing class. Fields: `@DateTimeFormat(iso = DATE) LocalDate startDate`, `@DateTimeFormat(pattern = "HH:mm") LocalTime startTime`, `LocalDate endDate`, `LocalTime endTime`, `Short carId`, `String destination`, `Integer version`, and `String action`. Add `TripPeriod toPeriod()`, which returns null if any part is missing
- [X] T052 [P] [US1] Create `src/main/java/de/lgln/dienstreise/trip/domain/TripValidationException.java` (carries a list of `(field, code)` pairs) and `src/main/java/de/lgln/dienstreise/trip/domain/CarAlreadyBookedException.java` (carries `carId` only; no data of other trips, FR-019)
- [X] T053 [US1] Create `src/main/java/de/lgln/dienstreise/trip/web/TripFormValidator.java`, a Spring `Validator` using the injected `Clock`. It has methods `validatePeriod(TripForm, Errors)` (V1–V4), `validateCar(TripForm, Errors)` (V5: `carId` in 1–3), and `validateDestination(TripForm, Errors)` (V7: trimmed not empty; V8: `≤ 200` characters and no chars matching `\p{Cc}`). It normalizes `destination` with `strip()`. Binding type-mismatch errors map to `trip.date.invalid` / `trip.time.invalid`. Makes T046 pass
- [X] T054 [US1] Create `src/main/java/de/lgln/dienstreise/trip/AvailabilityService.java` with `List<CarAvailability> availabilityFor(TripPeriod period, UUID excludeTripId)`, using `CarRepository.findAllByOrderByIdAsc()` and `TripRepository.existsOverlap(...)` (when `excludeTripId` is null) or `existsOverlapExcluding(...)` (otherwise), and `boolean isAvailable(Short carId, TripPeriod, UUID excludeTripId)`. Add record `src/main/java/de/lgln/dienstreise/trip/CarAvailability.java` `(Short carId, String label, boolean available)`, which exposes only the boolean (FR-019). Makes T047 pass
- [X] T055 [US1] Create `src/main/java/de/lgln/dienstreise/trip/TripService.java` with `@Transactional UUID create(CurrentUser user, TripCommand cmd)`. It re-validates V4 against `clock.instant()` and V6 via `AvailabilityService.isAvailable(..., null)`, throws `TripValidationException` / `CarAlreadyBookedException`, persists `new BusinessTrip(UUID.randomUUID(), user.subject(), car, period, destination.strip(), clock.instant())` with `saveAndFlush`, and logs event `trip.created` with `tripId` and `carId` only (no destination or owner in logs). Add record `src/main/java/de/lgln/dienstreise/trip/TripCommand.java` `(Short carId, TripPeriod period, String destination)`. Makes T048 pass
- [X] T056 [US1] Create `src/main/java/de/lgln/dienstreise/trip/web/TripWizardController.java` for the create routes in `contracts/ui-routes.md`:
  - `GET /trips/new` → view `trips/period`
  - `POST /trips/new/period` → validate V1–V4. Error: `trips/period`. OK: model `availability` + view `trips/car`. If the form carries a previously chosen `carId` (user went back): keep it pre-selected when it is still available; otherwise clear it and add `trip.car.noLongerAvailable` (edge case). A previously entered `destination` is carried along unchanged
  - `POST /trips/new/car` → `action=back`: `trips/period`. Re-validate V1–V4 (error → `trips/period`), then V5 and V6 via `AvailabilityService`. If the car is unavailable, clear `carId`, add `trip.car.booked`, and show `trips/car`. OK → `trips/destination`, with a previously entered `destination` pre-filled
  - `POST /trips` → `action=back`: `trips/car`. Validate all, then call `TripService.create`. `TripValidationException` with a V4 code → `trips/period`. `CarAlreadyBookedException` → `trips/car` with refreshed availability and `trip.car.booked`. `DataAccessException` → `trips/destination` with `globalError=trip.saveFailed`. Success → `redirect:/trips` with flash `flashSuccess=flash.created`

  **No data loss (constitution I)**: every step carries all values entered so far (period, `carId`, `destination`), in hidden inputs or in visible fields. "Zurück" never discards a value, and the user never has to re-enter data. Use a `formAction` model attribute (`/trips/new/...` or `/trips`) so the templates can be reused for edit in US3. Makes T049 pass
- [X] T057 [P] [US1] Create `src/main/resources/templates/trips/period.html` (layout, `h1` "Neue Dienstreise – Schritt 1 von 3: Zeitraum"). Include the error summary. Wrap the form (`method=post`, `th:action=${formAction}`, CSRF) in `fieldset` "Beginn" (startDate, startTime) and `fieldset` "Ende" (endDate, endTime) using `form-field` fragments in Bootstrap `row g-3` with `col-sm-6`, hidden inputs for `carId` and `destination` when set (values from later steps survive "Zurück"), and add a "Weiter" button `btn btn-primary` plus an "Abbrechen" link to `/trips`
- [X] T058 [P] [US1] Create `src/main/resources/templates/trips/car.html` (`h1` "Schritt 2 von 3: Fahrzeug wählen"). Show the `trip-summary` of the period, hidden inputs for the period and for `destination` when set. Pre-check the radio of `carId` when set and available. Add `<fieldset>` with `<legend>` "Fahrzeug wählen" and, for each `availability` entry, a `form-check` with `<input type="radio" name="carId" class="form-check-input" id="car-{id}" required>`, with `disabled` when unavailable, and a `<label class="form-check-label">` containing the car label plus, when unavailable, `<span class="badge text-bg-secondary">nicht verfügbar</span>`. Show a field error for `carId` linked via `aria-describedby`. If none are available, show `alert alert-warning` `trip.car.noneAvailable` and only a "Zeitraum ändern" button (`name=action value=back`); otherwise "Zurück" and "Weiter"
- [X] T059 [P] [US1] Create `src/main/resources/templates/trips/destination.html` (`h1` "Schritt 3 von 3: Reiseziel"). Show the `trip-summary` of period and car (review, AS5), hidden inputs for the period and `carId`, a `destination` text input via `form-field` with `maxlength="200"` and hint `hint.destination`, a `globalError` alert, and "Zurück" / "Speichern" buttons
- [X] T060 [US1] Create `src/main/java/de/lgln/dienstreise/trip/web/TripListController.java` with `GET /` → `redirect:/trips` and `GET /trips` → view `trips/list`. Create a minimal `src/main/resources/templates/trips/list.html` (`h1` "Meine Dienstreisen", `flash` fragment, `btn btn-primary` link "Neue Dienstreise" → `/trips/new`). The table follows in US2
- [X] T061 [US1] Run `./mvnw verify` and `./mvnw verify -Pe2e -Dit.test=RecordTripE2eTest`; fix until T046–T050 pass. Verify quickstart scenarios 1, 2 and 9 manually against `compose.dev.yaml` — *Automated: T046–T050 green (MockMvc + Playwright/Keycloak, keyboard-only journey, axe on all steps). Manual quickstart checks 1, 2, 9 (incl. screen reader) still open → docs/accessibility-audit.md.*

**Checkpoint**: MVP. Trips can be recorded with validation, availability display and accessible forms.

---

## Phase 4: User Story 4 - Prevent double booking of a car (Priority: P1)

**Goal**: No car can ever have two overlapping trips, even when users save at the same moment. Conflicts produce a clear message that doesn't disclose other users' data (FR-017–FR-019, SC-007).

**Independent Test**: User X books car A for 09:00–12:00. User Y is then rejected for car A at 11:00–13:00 but succeeds for car A at 12:00–14:00 and for car B at 10:00–11:00. Ten concurrent saves of overlapping trips for one car result in exactly one stored trip.

### Tests for User Story 4 (write first, must fail) ⚠️

- [X] T062 [P] [US4] Write `src/test/java/de/lgln/dienstreise/integration/OverlapRulesIT.java` implementing the spec's independent test via `TripService`. Seed anna car 1 on `2026-10-02 09:00–12:00` Berlin. Then ben on car 1 at 11:00–13:00 → `CarAlreadyBookedException`; ben on car 1 at 12:00–14:00 → stored (AS3); ben on car 2 at 10:00–11:00 → stored (AS4). Finally, a SQL check `SELECT count(*) FROM business_trip a JOIN business_trip b ON a.car_id=b.car_id AND a.id<b.id AND tstzrange(a.start_at,a.end_at,'[)') && tstzrange(b.start_at,b.end_at,'[)')` returns 0
- [X] T063 [P] [US4] Write `src/test/java/de/lgln/dienstreise/integration/ConcurrentBookingIT.java` (not transactional). Start 10 threads through a `CountDownLatch`, each calling `TripService.create` for car 1 with overlapping periods and distinct users; stub `AvailabilityService.isAvailable` to return true via `@MockitoSpyBean`, so that only the DB constraint decides. Assert exactly one success and nine `CarAlreadyBookedException`s (no other exception types), and that the overlap SQL from T062 returns 0 (AS7, FR-018, SC-007). Repeat 5 times with `@RepeatedTest`
- [X] T064 [P] [US4] Write `src/test/java/de/lgln/dienstreise/integration/RaceConditionWizardIT.java` (MockMvc). Ben has reached the destination step for car 2 at 10:00–11:00. Insert anna's overlapping trip for car 2 directly via the repository, then `POST /trips` as ben. Expect 200 with view `trips/car`, car 2's radio `disabled` with "nicht verfügbar", no `carId` checked, the text of `trip.car.booked`, and ben's period values kept (AS2). Then delete anna's trip and retry → 302 (edge case "retry succeeds")
- [X] T065 [P] [US4] Write `src/test/java/de/lgln/dienstreise/integration/NoDisclosureIT.java`. Seed anna's trip for car 1 with destination `Geheimziel-XYZ-4711` on `2026-10-02 09:00–12:00`. As ben, capture the response bodies and headers of:
  - `POST /trips/new/period` for 10:00–11:00
  - `POST /trips/new/car` with `carId=1` (manipulated)
  - `POST /trips` with `carId=1`

  None may contain `Geheimziel-XYZ-4711`, `anna-sub`, `Anna Beispiel`, `09:00` or `12:00` (FR-019, AS6)
- [X] T066 [P] [US4] Write `src/test/java/de/lgln/dienstreise/e2e/DoubleBookingE2eTest.java` (two browser contexts: anna and ben). Both reach step 3 with car 2 for overlapping periods; anna saves, then ben saves. Ben sees the car step with car 2 "nicht verfügbar" and the conflict message, and `assertNoAccessibilityViolations` passes on that page

### Implementation for User Story 4

- [X] T067 [US4] Create `src/main/java/de/lgln/dienstreise/trip/BookingConflictTranslator.java` with a static method `RuntimeException translate(DataIntegrityViolationException e, Short carId)`. It walks the cause chain to `org.postgresql.util.PSQLException`, and when `getSQLState()` is `"23P01"` and `getServerErrorMessage().getConstraint()` equals `"business_trip_no_overlap"`, returns `new CarAlreadyBookedException(carId)`; otherwise it returns `e`. In `TripService.create` (`src/main/java/de/lgln/dienstreise/trip/TripService.java`), wrap `saveAndFlush` in a try/catch that throws the translated exception. Makes T062 and T063 pass
- [X] T068 [US4] Add a Micrometer counter `dienstreise.booking.conflicts` (tag `source` = `precheck` | `constraint`) in `src/main/java/de/lgln/dienstreise/trip/TripService.java`, and log the event `trip.conflict` with `carId` only
- [X] T069 [US4] Harden `CarAlreadyBookedException` handling in `src/main/java/de/lgln/dienstreise/trip/web/TripWizardController.java`: on conflict at `POST /trips`, reload availability for the submitted period, set `form.carId = null`, add the global error `trip.car.booked`, and render `trips/car`. Make sure no message argument or model attribute contains data of other trips. Makes T064 and T065 pass
- [X] T070 [US4] Run `./mvnw verify` and `./mvnw verify -Pe2e -Dit.test=DoubleBookingE2eTest`; fix until T062–T066 pass. Verify quickstart scenarios 4, 5 and 6 manually — *Automated: T062–T066 green, incl. 5×10 concurrent bookings. Scenarios 4–6 are covered by DoubleBookingE2eTest/NoDisclosureIT; manual run still open.*

**Checkpoint**: Recording is safe under concurrency. US1 and US4 together form the P1 scope.

---

## Phase 5: User Story 2 - View my business trips in a list (Priority: P2)

**Goal**: Users see all of their own trips, sorted by start (earliest first), with car, start, destination, end and status, and a helpful empty state (FR-011–FR-015, SC-003, SC-004).

**Independent Test**: Seed anna with trips on three different days, inserted out of order, and ben with one trip. Anna's list shows exactly her three trips in ascending start order with all columns in German format. Ben sees only his trip. A new user sees the empty state.

### Tests for User Story 2 (write first, must fail) ⚠️

- [X] T071 [P] [US2] Write `src/test/java/de/lgln/dienstreise/integration/TripListIT.java` (MockMvc + jsoup via `Jsoup.parse(body)`):
  - Seed anna's trips at 2026-10-05, 2026-10-03 and 2026-10-04, plus two trips with an identical start where the second has a later `created_at`, and a trip for ben.
  - As anna: `GET /trips` has `table caption` "Meine Dienstreisen", headers `th[scope=col]` Fahrzeug, Beginn, Ziel, Ende, Status (AS2), rows in ascending start order with ties ordered by `created_at` (AS1, edge case), and cells formatted like `03.10.2026, 09:00` (FR-015). Ben's trip is absent (AS3).
  - With the clock set so that one trip is ongoing and one past, the status cells show "Läuft" / "Abgeschlossen" / "Geplant".
  - A new user sees the text "Sie haben noch keine Dienstreisen erfasst." and a link to `/trips/new` (AS4).
  - After `POST /trips` creates a trip at 2026-10-04 12:00, it appears between the 10-04 09:00 and 10-05 entries (AS5).
- [X] T072 [P] [US2] Write `src/test/java/de/lgln/dienstreise/integration/TripListPerfIT.java`. It runs only in the Maven profile `perf` (`./mvnw verify -Pperf`), not in the default build, because wall-clock assertions are not deterministic on shared machines (constitution II); run it before each release on a dedicated machine. Seed 500 trips for anna via `JdbcTemplate.batchUpdate` (non-overlapping, spread across the 3 cars), warm up once, then measure 5 × `GET /trips`. Assert each takes < 2 000 ms and the page contains 500 rows (SC-003)
- [X] T073 [P] [US2] Write `src/test/java/de/lgln/dienstreise/e2e/TripListE2eTest.java`. As anna with seeded trips: the list is visible, `assertNoAccessibilityViolations` passes, and at viewport 320×800 the page has no horizontal scrollbar (`document.documentElement.scrollWidth <= 320`, WCAG 1.4.10). The empty state for ben passes the axe check too

### Implementation for User Story 2

- [X] T074 [P] [US2] Create `src/main/java/de/lgln/dienstreise/trip/web/TripListItem.java`, a record `(UUID id, String carLabel, String start, String startDate, String destination, String end, TripStatus status, boolean editable)` (`start` = `@fmt.dateTime`, `startDate` = `@fmt.date`, used for accessible names) with factory `from(BusinessTrip, GermanDateTimeFormat, Instant now)`, where `editable = status == UPCOMING`
- [X] T075 [US2] Add `@Transactional(readOnly = true) List<TripListItem> listFor(CurrentUser user)` to `src/main/java/de/lgln/dienstreise/trip/TripService.java` (uses `findByOwnerSubjectOrderByStartAtAscCreatedAtAscIdAsc`). Change `TripListController.list` in `src/main/java/de/lgln/dienstreise/trip/web/TripListController.java` to put `trips` into the model
- [X] T076 [US2] Extend `src/main/resources/templates/trips/list.html`. With trips: `<div class="table-responsive"><table class="table table-striped align-middle">` with `<caption>Meine Dienstreisen</caption>`, `<th scope="col">` for Fahrzeug, Beginn, Ziel, Ende, Status and Aktionen, and a `<th scope="row">` cell for Beginn. Show the status as text badge `badge text-bg-*` with the text label (not colour only), and leave the "Aktionen" column cell empty for now. Without trips: `<p>` "Sie haben noch keine Dienstreisen erfasst." with the link "Neue Dienstreise erfassen" → `/trips/new` (FR-014). Destination is rendered only via `th:text`. Makes T071–T073 pass
- [X] T077 [US2] Run `./mvnw verify`, `./mvnw verify -Pe2e -Dit.test=TripListE2eTest` and `./mvnw verify -Pperf`; verify quickstart scenario 3 manually — *Automated: verify, e2e and perf (500 trips) green. Manual scenario 3 still open.*

**Checkpoint**: US1, US4 and US2 work together; the list reflects all recorded trips.

---

## Phase 6: User Story 3 - Change or remove a recorded trip (Priority: P3)

**Goal**: Users can edit all details of their own upcoming trips through the same wizard, or delete them after confirmation. Ongoing and past trips, and other users' trips, cannot be changed (FR-020–FR-023, SC-008).

**Independent Test**: Seed anna with an upcoming trip. Edit it to another day and car: the list shows the change in its new position. Delete it: after cancelling it remains, after confirming it is gone and ben can book the freed slot. A past trip and ben's trip cannot be edited or deleted, even via manipulated requests.

### Tests for User Story 3 (write first, must fail) ⚠️

- [X] T078 [P] [US3] Write `src/test/java/de/lgln/dienstreise/integration/TripServiceUpdateDeleteIT.java`:
  - `update(anna, id, version, cmd)`:
    - Moving the trip within its own slot or keeping it unchanged succeeds; it does not conflict with itself (US4 AS5).
    - Moving it into an overlap with ben's trip → `CarAlreadyBookedException`.
    - Moving the start into the past → `trip.start.notInFuture`.
    - When the stored trip has already started (clock advanced past start) → `TripReadOnlyException` (V10, edge case "start passes while edit form open").
    - A stale `version` → `TripStaleException` (V11).
    - ben updating anna's trip → `TripNotFoundException` (V9).
  - `delete(anna, id)`: removes the row. On a started trip → `TripReadOnlyException`. ben deleting anna's trip → `TripNotFoundException`, and the row still exists.
- [X] T079 [P] [US3] Write `src/test/java/de/lgln/dienstreise/integration/TripEditWizardIT.java` (MockMvc, anna):
  1. `GET /trips/{id}/edit` returns 200 with step 1 pre-filled with the stored start/end in Berlin local time, and `formAction` `/trips/{id}/edit/period` (AS1).
  2. `POST /trips/{id}/edit/period` with its own period → car step with its own car enabled and pre-selected (AS1).
  3. `POST /trips/{id}` with changed values and the correct `version` → 302 to `/trips`, flash `flash.updated`, and the list re-sorted (AS2).
  4. Invalid values give the same messages as creating (AS3).
  5. An ongoing trip on `GET /trips/{id}/edit` and on `POST /trips/{id}` → 302 to `/trips` with flash error `trip.readOnly`, row unchanged (AS6).
  6. Ben on any edit route of anna's trip → 404 with `trip.notFound`, row unchanged (AS7).
  7. A stale `version` → destination step with `trip.staleEdit`.
  8. When `TripService.update` throws `DataAccessResourceFailureException` (via `@MockitoSpyBean`), the destination step is re-rendered with `trip.saveFailed`, the entered data kept, and the row unchanged (FR-010).
- [X] T080 [P] [US3] Write `src/test/java/de/lgln/dienstreise/integration/TripDeleteIT.java` (MockMvc):
  1. `GET /trips/{id}/delete` shows car, period and destination, a form `POST /trips/{id}/delete` with button "Endgültig löschen", and a link "Abbrechen" → `/trips`.
  2. Following "Abbrechen" leaves the trip unchanged (AS5).
  3. `POST /trips/{id}/delete` → 302 to `/trips`, flash `flash.deleted`, row gone (AS4), and ben's `AvailabilityService` now reports the car as available for that period (edge case "slot freed").
  4. On an ongoing trip → flash error `trip.readOnly`, row kept (AS6).
  5. Ben → 404, row kept (AS7).
  6. `POST` without CSRF → 403.
- [X] T081 [P] [US3] Add a test to `src/test/java/de/lgln/dienstreise/integration/TripListIT.java`: upcoming trips have links "Bearbeiten" → `/trips/{id}/edit` and "Löschen" → `/trips/{id}/delete`, with accessible names via visually hidden text such as "Dienstreise nach Hannover am 01.10.2026 bearbeiten". Ongoing and past trips have no such links (AS6)
- [X] T082 [P] [US3] Write `src/test/java/de/lgln/dienstreise/e2e/EditDeleteTripE2eTest.java`. As anna, edit an upcoming trip via the keyboard (change the day and the car) and assert the new position in the list. Delete another trip: cancel, then confirm. Run `assertNoAccessibilityViolations` on the edit steps and the delete confirmation page. No timing assertion (SC-008 is measured in the usability test, T100)

### Implementation for User Story 3

- [X] T083 [P] [US3] Create `src/main/java/de/lgln/dienstreise/trip/domain/TripReadOnlyException.java` and `src/main/java/de/lgln/dienstreise/trip/domain/TripStaleException.java`
- [X] T084 [US3] Add the following to `src/main/java/de/lgln/dienstreise/trip/TripService.java`:
  - `@Transactional(readOnly = true) TripForm loadForEdit(CurrentUser, UUID id)`: V9 via `findByIdAndOwnerSubject`, else `TripNotFoundException`; V10 via `status(clock.instant()) != UPCOMING` → `TripReadOnlyException`; converts to Berlin local date and time
  - `@Transactional void update(CurrentUser, UUID id, int version, TripCommand cmd)`: V9, V10 on the stored trip, then the version check (V11 → `TripStaleException`), V4 on the new start, V6 with `excludeTripId = id`, `trip.change(...)`, `saveAndFlush` with `BookingConflictTranslator`, and log event `trip.updated`
  - `@Transactional void delete(CurrentUser, UUID id)`: V9, V10, `delete`, and log event `trip.deleted`

  Makes T078 pass
- [X] T085 [US3] Add edit routes to `src/main/java/de/lgln/dienstreise/trip/web/TripWizardController.java`: `GET /trips/{id}/edit`, `POST /trips/{id}/edit/period`, `POST /trips/{id}/edit/car`, and `POST /trips/{id}`. They reuse the create logic with `excludeTripId = id`, `formAction` set to the edit URLs, a heading prefix "Dienstreise bearbeiten", and hidden `version`. `TripReadOnlyException` → `redirect:/trips` with `flashError=trip.readOnly`; `TripStaleException` → `trips/destination` with `globalError=trip.staleEdit`; success → `redirect:/trips` with `flashSuccess=flash.updated`. Parametrize the `h1` texts in `trips/period.html`, `trips/car.html` and `trips/destination.html` via a `mode` model attribute (`create`/`edit`). Makes T079 pass
- [X] T086 [US3] Create `src/main/java/de/lgln/dienstreise/trip/web/TripDeleteController.java` with `GET /trips/{id}/delete` → view `trips/delete-confirm` (model: `TripListItem`; V9, V10) and `POST /trips/{id}/delete` → `TripService.delete` → `redirect:/trips` with `flashSuccess=flash.deleted`; `TripReadOnlyException` → `redirect:/trips` with `flashError=trip.readOnly`. Create `src/main/resources/templates/trips/delete-confirm.html`: `h1` "Dienstreise löschen?", `trip-summary` with destination, text "Die Dienstreise wird endgültig gelöscht.", `<form method="post">` with CSRF and `btn btn-danger` "Endgültig löschen", and `btn btn-outline-secondary` link "Abbrechen" → `/trips`. Makes T080 pass
- [X] T087 [US3] Fill the "Aktionen" column in `src/main/resources/templates/trips/list.html`. For `item.editable`, add links `btn btn-sm btn-outline-primary` "Bearbeiten" and `btn btn-sm btn-outline-danger` "Löschen", each with `<span class="visually-hidden">` "Dienstreise nach {destination} am {startDate}" for a unique accessible name. Otherwise the cell stays empty. Makes T081 pass
- [X] T088 [US3] Run `./mvnw verify` and `./mvnw verify -Pe2e -Dit.test=EditDeleteTripE2eTest`; verify quickstart scenarios 7 and 8 manually — *Automated: T078–T082 green. Manual scenarios 7 and 8 still open.*

**Checkpoint**: All four user stories are functional and independently tested.

---

## Phase 7: Automatic deletion after retention period (FR-024, cross-cutting)

**Purpose**: GDPR/DSGVO storage limitation. Trips are deleted automatically after a configurable retention period.

- [X] T089 [P] Write `src/test/java/de/lgln/dienstreise/integration/RetentionJobIT.java`. With `retention.period = P90D` and the clock at `2027-01-10T00:00:00Z`: a trip with `end_at` 2026-10-11T23:59Z (older than 90 days) is deleted by `RetentionJob.run()`, while trips ending at exactly the cutoff or later are kept (strictly `end_at < now - period`). Counter `dienstreise.retention.deleted` increases by the number deleted. A second run deletes 0 (idempotent)
- [X] T090 [P] Write `src/test/java/de/lgln/dienstreise/unit/ProdProfileStartupTest.java` (`ApplicationContextRunner` with profile `prod`). It fails with a message containing `DIENSTREISE_RETENTION_PERIOD` when `dienstreise.retention.period` is unset, and starts when it is `P30D`
- [X] T091 Create `src/main/java/de/lgln/dienstreise/retention/RetentionJob.java` (`@Component`) with `@Scheduled(cron = "${dienstreise.retention.cron}") @Transactional void run()`: `int n = tripRepository.deleteByEndAtBefore(clock.instant().minus(props.retention().period()))`, increment the Micrometer counter `dienstreise.retention.deleted` by `n`, and log `retention.run` with `deleted=n` and `cutoff`. No trip details are logged. Makes T089 and T090 pass
- [X] T092 Verify quickstart scenarios 10 and 12 manually — *Scenario 12 verified with the production image (refuses start without retention period / with two cars). Scenario 10 covered by RetentionJobIT; manual PT1M run still open.*

---

## Phase 8: Polish, Delivery & Cross-Cutting Concerns

**Purpose**: Container, deployment, pipeline security gates, documentation, and final validation (Principles III and IV)

- [X] T093 [P] Create `Dockerfile`:
  - Stage 1 `eclipse-temurin:25-jdk`: copy the wrapper and `pom.xml`, run `./mvnw -B dependency:go-offline`, copy `src`, run `./mvnw -B package -DskipTests`, and extract the layered jar with `java -Djarmode=tools -jar app.jar extract --layers --launcher`
  - Stage 2 `eclipse-temurin:25-jre`: non-root user `app` (uid 10001), copy the layers, `ENV TZ=UTC JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.io.tmpdir=/tmp"`, `EXPOSE 8080 8081`, `USER 10001`, and `ENTRYPOINT` the Spring Boot launcher

  Add `.dockerignore` (`target/`, `.git/`, `specs/`, `.claude/`, `.specify/`)
- [X] T094 [P] Create the Helm chart in `deploy/helm/dienstreise/`:
  - `Chart.yaml`
  - `values.yaml`: `image`, `replicaCount: 2`, `resources`, `ingress`, `env.retentionPeriod` (empty), `cars` (3 entries empty), `existingSecret` (keys `SPRING_DATASOURCE_PASSWORD`, `DIENSTREISE_OIDC_CLIENT_SECRET`)
  - `templates/deployment.yaml`: env from ConfigMap and Secret; `required` for `retentionPeriod` and each car with the message "Aufbewahrungsfrist/Fahrzeuge fehlen"; liveness `/actuator/health/liveness` and readiness `/actuator/health/readiness` on port 8081; `terminationGracePeriodSeconds: 40`; `securityContext` with `runAsNonRoot: true`, `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false` and `capabilities.drop: [ALL]`; an `emptyDir` mounted at `/tmp`
  - `templates/service.yaml` (port 8080 only; 8081 not exposed via ingress), `templates/ingress.yaml`, `templates/configmap.yaml`, `templates/pdb.yaml` (`minAvailable: 1`)
  - `values-prod.example.yaml` with comment "Aufbewahrungsfrist mit DSB abstimmen (FR-024)"
- [X] T095 [P] Add a Maven profile `security` to `pom.xml` with `cyclonedx-maven-plugin` (goal `makeBom` — single-module project; output `target/bom.json`), `spotbugs-maven-plugin` with the `findsecbugs-plugin` (fail on `High`), and `org.owasp:dependency-check-maven` (`failBuildOnCVSS=7`)
- [X] T096 [P] Create the platform-neutral pipeline scripts in `ci/`:
  - `ci/build-test.sh`: `./mvnw -B verify && ./mvnw -B verify -Pe2e`
  - `ci/security-scan.sh`: `./mvnw -B verify -Psecurity -DskipTests`; `semgrep scan --config p/java --config p/secrets --error`; `gitleaks detect --redact --exit-code 1`; `trivy fs --severity HIGH,CRITICAL --exit-code 1 .`
  - `ci/image-build.sh`: `docker build -t "$IMAGE" .`; `trivy image --severity HIGH,CRITICAL --exit-code 1 "$IMAGE"`; `trivy image --format cyclonedx -o target/image-bom.json "$IMAGE"`
  - `ci/helm-lint.sh`: `helm lint deploy/helm/dienstreise -f deploy/helm/dienstreise/values-prod.example.yaml`

  All scripts use `set -euo pipefail`
- [X] T097 [P] Create `renovate.json` (extends `config:recommended`; group Spring Boot, Testcontainers, Playwright; pin Docker digests; automerge disabled) at the repository root
- [X] T098 [P] Create `README.md` at the repository root: purpose, stack (link to `specs/001-business-trip-management/plan.md`), local start (link to `quickstart.md`), test commands, configuration (link to `contracts/configuration.md`), and open go-live items (car data, retention period with the DPO, IdP registration)
- [X] T099 [P] Create `docs/accessibility-audit.md`, a manual BITV 2.0 / WCAG 2.1 AA check protocol for the 6 screens (list, empty list, 3 wizard steps, delete confirmation). It covers keyboard only, NVDA + Firefox, zoom 200 %/400 %, reflow at 320 px, contrast check, and "nicht verfügbar" announcement, with a results table for SC-006
- [X] T100 [P] Create `docs/usability-test.md`, a usability test protocol for SC-001, SC-002 and SC-008 (constitution I: validate with real users). Include at least 5 representative employees who have not used the app, run on the dev or test environment with seed data. Tasks: (1) record a trip, (2) find it in the list, (3) move an upcoming trip to another day, (4) delete a trip. Measure time per task and first-attempt success without help. Targets: task 1 < 2 min, ≥ 90 % first-attempt success for task 1, tasks 3 and 4 < 1 min each. Include a results table and a findings section; findings become new spec or task items
- [X] T101 Security review pass: confirm `TemplateSafetyTest`, `NoDisclosureIT`, ownership tests (`TripEditWizardIT`, `TripDeleteIT`) and `SecurityConfigIT` all run in `./mvnw verify`, and that log output of a full E2E run contains no destination strings or `owner_subject` values (`grep` over the Failsafe logs in `target/failsafe-reports/`)
- [X] T102 Run `ci/build-test.sh`, `ci/security-scan.sh`, `ci/image-build.sh` and `ci/helm-lint.sh` locally; fix all failures and high/critical findings — *All four scripts green. Fixed: critical Tomcat CVE (override to 11.0.26). OWASP Dependency-Check could not run: version 13 needs an NVD API key (`NVD_API_KEY`); the script fails without it unless `SKIP_DEPENDENCY_CHECK=1` is set. Trivy (image jars + pom) found no high/critical issues.*
- [ ] T103 Run the complete `specs/001-business-trip-management/quickstart.md` validation (automated suites + scenarios 1–12) against `compose.dev.yaml` and record the results in `docs/accessibility-audit.md` (scenario 9) and in the PR description — *Open. Done so far: automated suites (42 unit, 85 integration/E2E), production image scenarios 11 and 12, dev quickstart startup and login redirect. Still manual: scenarios 1–10 in a browser, screen reader check, results in docs/accessibility-audit.md and PR description.*

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; **blocks all user stories**
- **US1 (Phase 3)**: depends on Foundational
- **US4 (Phase 4)**: depends on US1 (extends `TripService.create` and the wizard's save path)
- **US2 (Phase 5)**: depends on Foundational only for its tests (they seed data directly); T075/T076 extend the `TripListController` and `list.html` created in T060 (US1)
- **US3 (Phase 6)**: depends on US1 (reuses wizard templates/controller), US2 (action column in the list) and US4 (`BookingConflictTranslator`)
- **Retention (Phase 7)**: depends on Foundational only (uses `TripRepository.deleteByEndAtBefore`); can run in parallel with any story phase
- **Polish (Phase 8)**: T093–T099 can start after Foundational; T100 needs US1–US3 on a test environment; T101–T103 need all phases complete
- **CI/CD pipeline**: deliberately deferred (see plan.md Complexity Tracking). Until it exists, run `ci/build-test.sh` and `ci/security-scan.sh` locally before every merge

### Story completion order

```text
Setup → Foundational ─┬─→ US1 (P1, MVP) ─┬─→ US4 (P1) ─┐
                      │                   └─→ US2 (P2) ─┴─→ US3 (P3) ─→ Polish (T101–T103)
                      └─→ Retention (FR-024), Polish (T093–T099, T100)
```

### Within each phase

- Test tasks first; run them and confirm they fail (Red)
- Then domain types → services → controllers → templates (Green)
- Refactor with all tests green; run the phase's verification task last

## Parallel Opportunities

- **Setup**: T004, T005, T006 and T007 in parallel after T001
- **Foundational**:
  - All test tasks T013–T023, including T019, in parallel (after T009–T012)
  - Migrations T024–T026 in parallel
  - T029–T033 and T035 in parallel
  - T041 and T042 in parallel
- **US1**:
  - Tests T046–T050 in parallel
  - T051 and T052 in parallel
  - Templates T057–T059 in parallel once T056's model attributes are fixed
- **US4**: tests T062–T066 in parallel
- **US2**: tests T071–T073 in parallel; T074 in parallel with the tests
- **US3**: tests T078–T082 in parallel; T083 in parallel with the tests
- **Retention**: T089 and T090 in parallel. The whole phase can run alongside US2 or US3
- **Polish**: T093–T099 and T100 all in parallel

## Parallel Example: User Story 1

```bash
# Red: write all US1 tests together
Task: "TripFormValidatorTest in src/test/java/de/lgln/dienstreise/unit/TripFormValidatorTest.java"
Task: "AvailabilityServiceIT in src/test/java/de/lgln/dienstreise/integration/AvailabilityServiceIT.java"
Task: "TripServiceCreateIT in src/test/java/de/lgln/dienstreise/integration/TripServiceCreateIT.java"
Task: "TripWizardCreateIT in src/test/java/de/lgln/dienstreise/integration/TripWizardCreateIT.java"
Task: "RecordTripE2eTest in src/test/java/de/lgln/dienstreise/e2e/RecordTripE2eTest.java"

# Green: independent types, then templates
Task: "TripForm in src/main/java/de/lgln/dienstreise/trip/web/TripForm.java"
Task: "Exceptions in src/main/java/de/lgln/dienstreise/trip/domain/"
Task: "period.html / car.html / destination.html in src/main/resources/templates/trips/"
```

## Parallel Example: User Story 4

```bash
Task: "OverlapRulesIT in src/test/java/de/lgln/dienstreise/integration/OverlapRulesIT.java"
Task: "ConcurrentBookingIT in src/test/java/de/lgln/dienstreise/integration/ConcurrentBookingIT.java"
Task: "RaceConditionWizardIT in src/test/java/de/lgln/dienstreise/integration/RaceConditionWizardIT.java"
Task: "NoDisclosureIT in src/test/java/de/lgln/dienstreise/integration/NoDisclosureIT.java"
Task: "DoubleBookingE2eTest in src/test/java/de/lgln/dienstreise/e2e/DoubleBookingE2eTest.java"
```

---

## Implementation Strategy

### MVP first

1. Phase 1 Setup → Phase 2 Foundational
2. Phase 3 US1 → **stop and validate** (quickstart scenarios 1, 2, 9). This is a demoable MVP: trips can be recorded.
3. Phase 4 US4 → recording is safe under concurrent use. **P1 scope complete; the minimum for pilot use.**

### Incremental delivery

1. P1 (US1 + US4) → demo / pilot
2. US2 → users see their trips
3. US3 → users correct or delete trips
4. Retention + Polish → production readiness. **Go-live gate**: retention period agreed with the DPO, car data provided, and IdP client registered (the app refuses to start in `prod` without them)

### Parallel team strategy

After Foundational: developer A takes US1 → US4 → US3; developer B takes Retention (Phase 7) and Polish T093–T099, then US2 once T060 exists.

---

## Notes

- [P] = different files, no dependency on incomplete tasks
- Every acceptance scenario of spec.md is covered by at least one test task (AS references in the task text)
- Never call `Instant.now()` directly; always use the injected `Clock` (enforced by T022)
- Never log destinations or owner subjects (checked in T101)
- Commit after each Red/Green pair; open a PR per phase for review (constitution: review by at least one other person)

---

## Implementation Notes (/speckit-implement, 2026-09-27)

Deviations from the task text, all verified by the test suite:

- **T001**: Spring Boot 4 splits auto-configuration into modules. Used the Boot 4 starters
  `spring-boot-starter-webmvc`, `-security-oauth2-client`, `-flyway`, `-session-jdbc`,
  `-opentelemetry` (brings Micrometer OTLP + OTel bridge), `-webmvc-test`, `-security-test`, and
  Testcontainers 2 artifacts `testcontainers-postgresql` / `testcontainers-junit-jupiter`.
  `jsoup` added for HTML assertions, `micrometer-registry-prometheus` for `/actuator/prometheus`.
  `postgresql` has compile scope (T067 reads `PSQLException`). `tomcat.version` is overridden to
  11.0.26 because Trivy found critical CVE-2026-65182 in the BOM-managed 11.0.24.
- **T004**: `.editorconfig` uses 2 spaces for Java, matching google-java-format enforced by Spotless (T003).
- **T006**: Keycloak only supports wildcards at the end of redirect URIs; the dev realm uses
  `http://localhost:8080/*`. The test realm (T044) uses `*`.
- **T012**: A non-empty `issuer-uri` always triggers OIDC discovery in Spring Boot. Tests
  therefore provide the dummy client registration as a `ClientRegistrationRepository` bean
  (`support/TestOidcConfig`), which makes the auto-configuration back off; the YAML keeps the
  environment-variable names for cars.
- **T028**: The prod-only retention check is a separate `@Profile("prod")` bean (`ProdStartupGuard`).
- **T038**: An additional `@Order(1)` filter chain permits `/actuator/**`, which only exists on
  the management port (the main chain would otherwise redirect probes to the IdP).
- **T040**: npm WebJars keep files under `dist/`: `/webjars/bootstrap/dist/css/bootstrap.min.css`.
  The logout button uses `btn-outline-dark` (axe: `btn-outline-secondary` had 4.44:1 on the navbar).
- **T055/T067**: Concurrent inserts can **deadlock** on the exclusion constraint (40P01). Bookings
  of one car are serialized with a transaction-scoped advisory lock (`CarBookingLock`); see
  research.md R5 addendum. Conflicts are shown as field error on `carId` (in the error summary).
- **T076**: Column header "Ziel" (`label.destinationShort`); `.table-responsive` is
  `position: relative` so the visually hidden action texts do not break reflow at 320 px.
- **T095**: CycloneDX goal `makeBom` with its own execution id (the Boot parent already embeds an
  SBOM in the jar at `META-INF/sbom/application.cdx.json`).
- **T096**: `ci/lib.sh` runs Semgrep, gitleaks, Trivy and Helm from pinned images when the tools
  are not installed. gitleaks scans history **and** working tree; Trivy uses `--offline-scan`
  with the local Maven cache (Maven Central rate-limits BOM resolution).
- **Configuration**: `OTEL_EXPORT_ENABLED` switches OTLP export on (contracts/configuration.md).

