#!/usr/bin/env bash
# Shared helpers: run a tool natively if installed, otherwise from its pinned container image.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SEMGREP_IMAGE="semgrep/semgrep:1.178.0"
GITLEAKS_IMAGE="zricethezav/gitleaks:v8.30.1"
TRIVY_IMAGE="aquasec/trivy:0.74.0"
HELM_IMAGE="alpine/helm:4.3.0"

run_tool() {
  local tool="$1" image="$2"
  shift 2
  if command -v "$tool" >/dev/null 2>&1; then
    "$tool" "$@"
  else
    mkdir -p "${HOME}/.cache/trivy"
    # Run as the calling user so files written to the workspace (e.g. SBOMs) stay deletable.
    docker run --rm -v "$ROOT:/src" -w /src \
      --user "$(id -u):$(id -g)" \
      --group-add "$(stat -c %g /var/run/docker.sock)" \
      -e HOME=/tmp -e TRIVY_CACHE_DIR=/cache \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -v "${HOME}/.cache/trivy:/cache" \
      -v "${HOME}/.m2:/tmp/.m2:ro" \
      "$image" "$@"
  fi
}
