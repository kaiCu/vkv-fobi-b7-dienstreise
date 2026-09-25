#!/usr/bin/env bash
# SAST (SpotBugs/FindSecBugs, Semgrep), SCA (OWASP Dependency-Check, Trivy), secret detection
# (gitleaks) and SBOM (target/bom.json). High/critical findings fail the script.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
cd "$ROOT"

# OWASP Dependency-Check needs an NVD API key: https://nvd.nist.gov/developers/request-an-api-key
if [[ -n "${NVD_API_KEY:-}" ]]; then
  ./mvnw -B verify -Psecurity -DskipTests -DnvdApiKey="$NVD_API_KEY"
elif [[ "${SKIP_DEPENDENCY_CHECK:-}" == "1" ]]; then
  echo "WARNING: OWASP Dependency-Check skipped (no NVD_API_KEY); Trivy below still scans dependencies." >&2
  ./mvnw -B verify -Psecurity -DskipTests -Ddependency-check.skip=true
else
  echo "ERROR: NVD_API_KEY is not set. Request a key or set SKIP_DEPENDENCY_CHECK=1 explicitly." >&2
  exit 1
fi
run_tool semgrep "$SEMGREP_IMAGE" semgrep scan --config p/java --config p/secrets --error \
  --exclude target --exclude specs --metrics off
# git history and the current working tree
run_tool gitleaks "$GITLEAKS_IMAGE" detect --source . --redact --exit-code 1 --no-banner
run_tool gitleaks "$GITLEAKS_IMAGE" dir . --redact --exit-code 1 --no-banner
# --offline-scan resolves Maven BOMs from ~/.m2 (run the build first) instead of Maven Central
run_tool trivy "$TRIVY_IMAGE" fs --severity HIGH,CRITICAL --exit-code 1 --skip-dirs target \
  --offline-scan .
