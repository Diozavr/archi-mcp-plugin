#!/usr/bin/env bash
set -euo pipefail

API_PORT="${ARCHI_MCP_PORT:-8765}"
API_HOST="${ARCHI_MCP_HOST:-127.0.0.1}"
BASE="http://${API_HOST}:${API_PORT}/mcp"

CURL_BIN="${CURL_BIN:-curl}"

echo "[MCP] Base: $BASE"

run() {
  local desc="$1"; shift
  echo "\n[MCP] $desc"
  "$@" | jq .
}

curl_json() { "$CURL_BIN" -sS -H 'Content-Type: application/json' "$@"; }

# Ping
run "status/ping" curl_json -d '{"jsonrpc":"2.0","id":1,"method":"status/ping"}' "$BASE"

# List tools
run "tools/list" curl_json -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' "$BASE"

# Call status via tools/call
run "tools/call status" curl_json -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"status"}}' "$BASE"

