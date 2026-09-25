#!/usr/bin/env bash
# Lints the chart and renders it with the example production values.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
cd "$ROOT"

VALUES=deploy/helm/dienstreise/values-prod.example.yaml
run_tool helm "$HELM_IMAGE" lint deploy/helm/dienstreise -f "$VALUES"
run_tool helm "$HELM_IMAGE" template dienstreise deploy/helm/dienstreise -f "$VALUES" >/dev/null
