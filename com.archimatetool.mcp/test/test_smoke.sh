#!/usr/bin/env bash
set -euo pipefail

# Extended smoke test for Archi MCP Plugin (localhost/WSL)
API_PORT="${ARCHI_MCP_PORT:-8765}"

is_wsl() { grep -qiE 'microsoft|wsl' /proc/sys/kernel/osrelease 2>/dev/null || [ -n "${WSL_DISTRO_NAME:-}" ]; }

pick_curl() {
  if is_wsl && command -v curl.exe >/dev/null 2>&1; then
    echo "curl.exe"
  else
    echo "curl"
  fi
}

CURL_BIN="${CURL_BIN:-$(pick_curl)}"

# Resolve host: if using Windows curl.exe from WSL, 127.0.0.1 works (Windows loopback);
# otherwise use nameserver from /etc/resolv.conf to reach Windows host from WSL.
detect_host() {
  local host="127.0.0.1"
  if [ "$CURL_BIN" != "curl.exe" ] && is_wsl; then
    local ns
    ns=$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf 2>/dev/null || true)
    if [ -n "$ns" ]; then host="$ns"; fi
  fi
  echo "$host"
}

API_HOST="${ARCHI_MCP_HOST:-$(detect_host)}"
BASE="http://${API_HOST}:${API_PORT}"

echo "[SMOKE] Base: ${BASE}"

run() {
  local desc="$1"; shift
  echo "\n[SMOKE] ${desc}"
  "$@" | jq .
}

curl_json() { "$CURL_BIN" -sS -H 'Content-Type: application/json' "$@"; }

# Wait briefly for server to be ready
wait_ready() {
  local tries=20
  local delay=0.5
  while [ $tries -gt 0 ]; do
    code=$("$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/status" || true)
    if [ "$code" = "200" ]; then return 0; fi
    tries=$((tries-1))
    sleep "$delay"
  done
  echo "[SMOKE] Server not reachable at ${BASE}/status" >&2
  return 1
}

wait_ready

# 1) Service status
run "GET /status" curl_json "${BASE}/status"

# 2) OpenAPI
run "GET /openapi.json" curl_json "${BASE}/openapi.json"

# 3) Types
run "GET /types" curl_json "${BASE}/types"

# MCP JSON-RPC
run "RPC initialize" curl_json -X POST -d '{"jsonrpc":"2.0","id":1,"method":"initialize"}' "${BASE}/mcp"
run "RPC notifications/initialized" curl_json -X POST -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' "${BASE}/mcp"
run "RPC tools/list" curl_json -X POST -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' "${BASE}/mcp"
run "RPC tools/call status" curl_json -X POST -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"status","args":{}}}' "${BASE}/mcp"
run "RPC tools/call method not found" curl_json -X POST -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"bogus","args":{}}}' "${BASE}/mcp"
code=$("$CURL_BIN" -s -o /dev/null -w "%{http_code}" -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"status","args":{}}}' "${BASE}/mcp")
if [ "$code" != "204" ]; then echo "[SMOKE] Expected 204 for notification, got $code"; fi

# 4) Folders (may be 409 when no active model)
code=$("$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/folders")
if [ "$code" = "409" ]; then
  echo "\n[SMOKE] No active model (HTTP 409). Model-dependent checks are skipped."
  exit 0
fi
run "GET /folders" curl_json "${BASE}/folders"

# 5) Views
VID0=$(curl_json "${BASE}/views" | jq -r '.[0].id // empty')
run "GET /views" curl_json "${BASE}/views"
VID1=$(curl_json -X POST -d '{"type":"ArchimateDiagramModel","name":"Smoke View"}' "${BASE}/views" | jq -r '.id')
run "GET /views/${VID1}" curl_json "${BASE}/views/${VID1}"
run "GET /views/${VID1}/content" curl_json "${BASE}/views/${VID1}/content"
run "GET /views/content?id=${VID1}" curl_json "${BASE}/views/content?id=${VID1}"

# 6) Elements
E1=$(curl_json -X POST -d '{"type":"BusinessActor","name":"S1"}' "${BASE}/elements" | jq -r '.id')
E2=$(curl_json -X POST -d '{"type":"BusinessRole","name":"S2"}' "${BASE}/elements" | jq -r '.id')
run "GET /elements/${E1}" curl_json "${BASE}/elements/${E1}"
run "GET /elements/${E1}?include=relations&includeElements=true" curl_json "${BASE}/elements/${E1}?include=relations&includeElements=true"
run "GET /elements/${E1}/relations?direction=out" curl_json "${BASE}/elements/${E1}/relations?direction=out"
run "GET /elements/${E1}/relations?direction=in" curl_json "${BASE}/elements/${E1}/relations?direction=in"
run "GET /elements/${E1}/relations?direction=both&includeElements=true" curl_json "${BASE}/elements/${E1}/relations?direction=both&includeElements=true"
run "PATCH /elements/${E1}" curl_json -X PATCH -d '{"name":"S1-renamed"}' "${BASE}/elements/${E1}"

# 7) Relations
REL=$(curl_json -X POST -d '{"type":"AssociationRelationship","sourceId":"'${E1}'","targetId":"'${E2}'","name":"R"}' "${BASE}/relations" | jq -r '.id')
run "GET /relations/${REL}" curl_json "${BASE}/relations/${REL}"
run "PATCH /relations/${REL}" curl_json -X PATCH -d '{"name":"R-renamed"}' "${BASE}/relations/${REL}"

# 8) View operations
OBJ1=$(curl_json -X POST -d '{"viewId":"'${VID1}'","elementId":"'${E1}'"}' "${BASE}/views/add-element" | jq -r '.objectId')
OBJ2=$(curl_json -X POST -d '{"elementId":"'${E2}'","parentObjectId":'${OBJ1}',"bounds":{"x":10,"y":10,"w":40,"h":40}}' "${BASE}/views/${VID1}/add-element" | jq -r '.objectId')
run "PATCH /views/${VID1}/objects/${OBJ2}/bounds" curl_json -X PATCH -d '{"x":20,"y":20,"w":50,"h":50}' "${BASE}/views/${VID1}/objects/${OBJ2}/bounds"
run "PATCH /views/${VID1}/objects/${OBJ2}/move root" curl_json -X PATCH -d '{"parentObjectId":0}' "${BASE}/views/${VID1}/objects/${OBJ2}/move"
run "PATCH /views/${VID1}/objects/${OBJ2}/move back" curl_json -X PATCH -d '{"parentObjectId":'${OBJ1}',"keepExistingConnection":true}' "${BASE}/views/${VID1}/objects/${OBJ2}/move"
run "POST /views/${VID1}/add-relation auto" curl_json -X POST -d '{"relationId":"'${REL}'","policy":"auto"}' "${BASE}/views/${VID1}/add-relation"
run "POST /views/${VID1}/add-relation explicit" curl_json -X POST -d '{"relationId":"'${REL}'","sourceObjectId":'${OBJ1}',"targetObjectId":'${OBJ2}',"suppressWhenNested":true}' "${BASE}/views/${VID1}/add-relation"
run "GET /views/${VID1}/content" curl_json "${BASE}/views/${VID1}/content"
run "DELETE /views/${VID1}/objects/${OBJ2}" curl_json -X DELETE "${BASE}/views/${VID1}/objects/${OBJ2}"

# 9) View image
"$CURL_BIN" -sS "${BASE}/views/${VID1}/image?format=png&scale=1.0&bg=transparent&margin=0" -o /tmp/view.png -D /tmp/headers.txt > /dev/null
grep -iq 'image/png' /tmp/headers.txt
[ -s /tmp/view.png ]
code=$("$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/views/${VID1}/image?format=svg")
if [ "$code" != "400" ]; then echo "Unexpected SVG response code: $code"; exit 1; fi

# 10) Search
run "GET /search?q=Actor" curl_json "${BASE}/search?q=Actor"
run "GET /search with params" curl_json "${BASE}/search?q=Actor&kind=element&limit=1&offset=0&debug=true"

# 11) Save model
run "POST /model/save" curl_json -X POST "${BASE}/model/save"

# 12) Script stubs
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/script/engines" | grep -q '^501$'
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" -X POST "${BASE}/script/run" | grep -q '^501$'

# 13) Error checks
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/elements/bogus" | grep -q '^404$'
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/views/bogus" | grep -q '^404$'
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" -X POST "${BASE}/status" | grep -q '^405$'

# 14) Cleanup
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/relations/${REL}" | grep -q '^204$'
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/elements/${E1}" | grep -q '^204$'
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/elements/${E2}" | grep -q '^204$'
"$CURL_BIN" -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/views/${VID1}" | grep -q '^204$'

echo "\n[SMOKE] Flow completed."
