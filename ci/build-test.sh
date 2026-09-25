#!/usr/bin/env bash
# Unit, integration and architecture tests, then browser E2E tests with accessibility checks.
set -euo pipefail
cd "$(dirname "$0")/.."

./mvnw -B verify
./mvnw -B verify -Pe2e
