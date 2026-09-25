# Dienstreise

Beschäftigte melden sich mit dem Identity Provider der Organisation an und buchen eines von
drei Poolfahrzeugen für eine Dienstreise. Sie sehen ihre eigenen Reisen in einer Liste und
können geplante Reisen ändern oder löschen. Doppelbuchungen sind auch bei gleichzeitiger
Nutzung ausgeschlossen; Reisen werden nach einer konfigurierbaren Aufbewahrungsfrist
automatisch gelöscht.

- Spezifikation: [specs/001-business-trip-management/spec.md](specs/001-business-trip-management/spec.md)
- Architektur und Stack: [plan.md](specs/001-business-trip-management/plan.md),
  Entscheidungen in [research.md](specs/001-business-trip-management/research.md)

## Stack

Java 25, Spring Boot 4.0 (Web MVC, Thymeleaf, Security/OIDC, Data JPA, Session JDBC,
Actuator, OpenTelemetry), Bootstrap 5.3 (selbst gehostet als WebJar), PostgreSQL 17 mit
Flyway. Auslieferung als OCI-Image mit Helm-Chart.

## Lokal starten

Voraussetzungen: JDK 25, Docker mit Compose. Details und manuelle Prüfszenarien:
[quickstart.md](specs/001-business-trip-management/quickstart.md).

```bash
docker compose -f compose.dev.yaml up -d          # PostgreSQL + Keycloak (anna/anna, ben/ben)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# http://localhost:8080
```

## Tests

```bash
./mvnw verify               # Unit-, Integrations- (Testcontainers) und Architekturtests
./mvnw verify -Pe2e         # zusätzlich Browser-Tests (Playwright) mit axe-Barrierefreiheitsprüfung
./mvnw verify -Pperf        # Listen-Performance (SC-003), auf dedizierter Maschine vor Releases
./mvnw verify -Psecurity -DskipTests   # SAST, Abhängigkeits-Scan, SBOM (target/bom.json)
./mvnw spotless:apply       # Formatierung (google-java-format)
```

Die Pipeline-Schritte liegen plattformneutral in `ci/` (`build-test.sh`, `security-scan.sh`,
`image-build.sh`, `helm-lint.sh`). Bis eine CI/CD-Plattform festgelegt ist, müssen sie vor
jedem Merge lokal laufen (siehe plan.md, Complexity Tracking).

## Konfiguration

Alle Werte kommen aus Umgebungsvariablen:
[contracts/configuration.md](specs/001-business-trip-management/contracts/configuration.md).
Das Helm-Chart liegt in `deploy/helm/dienstreise`, ein Beispiel für Produktionswerte in
`values-prod.example.yaml`.

## Offene Punkte vor dem Go-live

- Namen und Kennzeichen der drei Fahrzeuge (Fachbereich)
- Aufbewahrungsfrist mit dem Datenschutzbeauftragten festlegen (FR-024); ohne
  `DIENSTREISE_RETENTION_PERIOD` startet die Anwendung im Profil `prod` nicht
- Client-Registrierung beim Identity Provider (Issuer-URL, Client-ID, Secret)
- CI/CD-Pipeline, automatisiertes Deployment und Datenbank-Provisionierung als Code
- Manueller BITV-Test ([docs/accessibility-audit.md](docs/accessibility-audit.md)) und
  Usability-Test ([docs/usability-test.md](docs/usability-test.md))
