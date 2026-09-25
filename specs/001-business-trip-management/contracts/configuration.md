# Contract: Runtime Configuration

**Feature**: [../spec.md](../spec.md) | **Research**: [../research.md](../research.md) (R6–R8, R11)

All configuration comes from environment variables (Twelve-Factor, Principle III). Secrets are
injected from the organization's secret store as Kubernetes Secrets and never committed
(Principle IV). The application validates configuration at startup and refuses to start
(non-zero exit, clear log message) if a required value is missing or invalid.

| Variable | Required | Example | Validation / Notes |
|----------|----------|---------|--------------------|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` | `prod` enables the strict checks below; `dev` supplies local defaults |
| `SPRING_DATASOURCE_URL` | yes | `jdbc:postgresql://db:5432/dienstreise` | PostgreSQL ≥ 16 with extension `btree_gist` available |
| `SPRING_DATASOURCE_USERNAME` | yes | `dienstreise` | |
| `SPRING_DATASOURCE_PASSWORD` | yes (secret) | — | |
| `DIENSTREISE_OIDC_ISSUER_URI` | yes | `https://login.example.org/realms/lgln` | OIDC discovery must succeed at startup |
| `DIENSTREISE_OIDC_CLIENT_ID` | yes | `dienstreise` | |
| `DIENSTREISE_OIDC_CLIENT_SECRET` | yes (secret) | — | |
| `DIENSTREISE_CARS_0_NAME` … `DIENSTREISE_CARS_2_NAME` | yes | `VW ID.3 (blau)` | exactly three, non-blank, ≤ 80 chars (FR-002, FR-003) |
| `DIENSTREISE_CARS_0_LICENSE_PLATE` … `_2_` | yes | `H-LG 1234` | non-blank, unique, ≤ 15 chars |
| `DIENSTREISE_RETENTION_PERIOD` | yes in `prod` | `P90D` | ISO-8601 duration > 0 (FR-024). **Value to be agreed with the DPO before go-live.** |
| `DIENSTREISE_RETENTION_CRON` | no | `0 0 * * * *` | Spring cron; default hourly |
| `DIENSTREISE_SESSION_TIMEOUT` | no | `30m` | Idle session timeout |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | no | `http://otel-collector:4318` | Tracing/metrics export; disabled if unset |
| `SERVER_PORT` / `MANAGEMENT_SERVER_PORT` | no | `8080` / `8081` | Application / probes and metrics |

The car variables are mapped with explicit placeholders in `application.yaml`
(`license-plate: ${DIENSTREISE_CARS_0_LICENSE_PLATE}`). Spring's automatic mapping of
environment variables would read `_LICENSE_PLATE` as a nested property `license.plate` and
silently not bind.

Time zone for input interpretation and display is fixed to `Europe/Berlin` (FR-015) and is
not configurable; the JVM itself runs in UTC.
