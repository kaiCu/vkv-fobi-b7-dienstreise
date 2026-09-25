#!/usr/bin/env bash
# Builds the OCI image, scans it and writes its SBOM to target/image-bom.json.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
cd "$ROOT"

IMAGE="${IMAGE:-dienstreise:local}"
docker build -t "$IMAGE" .
run_tool trivy "$TRIVY_IMAGE" image --severity HIGH,CRITICAL --exit-code 1 "$IMAGE"
mkdir -p target
run_tool trivy "$TRIVY_IMAGE" image --format cyclonedx -o target/image-bom.json "$IMAGE"
