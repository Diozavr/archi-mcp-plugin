#!/usr/bin/env bash
set -euo pipefail

# Simple smoke test for Archi MCP Plugin (localhost only)

API_PORT="${ARCHI_MCP_PORT:-8765}"
BASE="http://127.0.0.1:${API_PORT}"

echo "[SMOKE] Base: ${BASE}"

run() {
  local desc="$1"; shift
  echo "\n[SMOKE] ${desc}"
  "$@" | jq .
}

curl_json() { curl -sS -H 'Content-Type: application/json' "$@"; }

# 1) Service status
run "GET /status" curl_json "${BASE}/status"

# 2) OpenAPI
run "GET /openapi.json" curl_json "${BASE}/openapi.json"

# 3) Types
run "GET /types" curl_json "${BASE}/types"

# 4) Folders (may be 409 when no active model)
code=$(curl -s -o /dev/null -w "%{http_code}" "${BASE}/folders")
if [ "$code" = "409" ]; then
  echo "\n[SMOKE] No active model (HTTP 409). Model-dependent checks are skipped."
  exit 0
fi
run "GET /folders" curl_json "${BASE}/folders"

echo "\n[SMOKE] Basic checks passed."


