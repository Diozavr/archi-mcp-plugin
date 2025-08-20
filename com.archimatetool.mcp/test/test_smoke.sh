#!/usr/bin/env bash
set -euo pipefail

# Batch-oriented smoke test for Archi MCP Plugin (localhost/WSL)
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

# 1) Basic endpoints
run "GET /status" curl_json "${BASE}/status"
run "GET /openapi.json" curl_json "${BASE}/openapi.json"
run "GET /types" curl_json "${BASE}/types"

# 2) Folders (may be 409 when no active model)
code=$("$CURL_BIN" -s -o /dev/null -w "%{http_code}" "${BASE}/folders")
if [ "$code" = "409" ]; then
  echo "\n[SMOKE] No active model (HTTP 409). Model-dependent checks are skipped."
  exit 0
fi
run "GET /folders" curl_json "${BASE}/folders"

# 3) Batch element operations
ELEMS=$(curl_json -X POST -d '[{"type":"BusinessActor","name":"E1"},{"type":"BusinessRole","name":"E2"},{"type":"BusinessProcess","name":"E3"},{"type":"BusinessFunction","name":"E4"},{"type":"BusinessEvent","name":"E5"}]' "${BASE}/elements")
E1=$(echo "$ELEMS" | jq -r '.[0].id')
E2=$(echo "$ELEMS" | jq -r '.[1].id')
E3=$(echo "$ELEMS" | jq -r '.[2].id')
E4=$(echo "$ELEMS" | jq -r '.[3].id')
E5=$(echo "$ELEMS" | jq -r '.[4].id')
run "GET /elements?ids=..." curl_json "${BASE}/elements?ids=${E1}&ids=${E2}&ids=${E3}&ids=${E4}&ids=${E5}"
run "PATCH /elements" curl_json -X PATCH -d '[{"id":"'${E1}'","name":"E1r"},{"id":"'${E2}'","name":"E2r"},{"id":"'${E3}'","name":"E3r"}]' "${BASE}/elements"

# 4) Batch relation operations
RELS=$(curl_json -X POST -d '[{"type":"AssociationRelationship","sourceId":"'${E1}'","targetId":"'${E2}'"},{"type":"AssociationRelationship","sourceId":"'${E2}'","targetId":"'${E3}'"},{"type":"AssociationRelationship","sourceId":"'${E3}'","targetId":"'${E4}'"},{"type":"AssociationRelationship","sourceId":"'${E4}'","targetId":"'${E5}'"}]' "${BASE}/relations")
R1=$(echo "$RELS" | jq -r '.[0].id')
R2=$(echo "$RELS" | jq -r '.[1].id')
R3=$(echo "$RELS" | jq -r '.[2].id')
R4=$(echo "$RELS" | jq -r '.[3].id')
run "GET /relations?ids=..." curl_json "${BASE}/relations?ids=${R1}&ids=${R2}&ids=${R3}&ids=${R4}"
run "PATCH /relations" curl_json -X PATCH -d '[{"id":"'${R1}'","name":"R1r"},{"id":"'${R2}'","name":"R2r"}]' "${BASE}/relations"

# 5) View and view objects
VID=$(curl_json -X POST -d '{"type":"ArchimateDiagramModel","name":"Smoke View"}' "${BASE}/views" | jq -r '.id')
OBJS=$(curl_json -X POST -d '[{"elementId":"'${E1}'","bounds":{"x":10,"y":10,"w":40,"h":40}},{"elementId":"'${E2}'"},{"elementId":"'${E3}'"},{"elementId":"'${E4}'"},{"elementId":"'${E5}'"}]' "${BASE}/views/${VID}/add-element")
O1=$(echo "$OBJS" | jq -r '.[0].objectId')
O2=$(echo "$OBJS" | jq -r '.[1].objectId')
O3=$(echo "$OBJS" | jq -r '.[2].objectId')
O4=$(echo "$OBJS" | jq -r '.[3].objectId')
O5=$(echo "$OBJS" | jq -r '.[4].objectId')
run "POST /views/${VID}/add-relation" curl_json -X POST -d '[{"relationId":"'${R1}'"},{"relationId":"'${R2}'"},{"relationId":"'${R3}'"},{"relationId":"'${R4}'"}]' "${BASE}/views/${VID}/add-relation"
run "PATCH /views/${VID}/objects/bounds" curl_json -X PATCH -d '[{"objectId":'${O1}',"x":20},{"objectId":'${O2}',"y":20},{"objectId":'${O3}',"w":80,"h":80}]' "${BASE}/views/${VID}/objects/bounds"
run "PATCH /views/${VID}/objects/move" curl_json -X PATCH -d '[{"objectId":'${O4}',"parentObjectId":'${O1}'},{"objectId":'${O5}',"parentObjectId":'${O1}'}]' "${BASE}/views/${VID}/objects/move"
run "DELETE /views/${VID}/objects" curl_json -X DELETE -d '[{"objectId":'${O1}'},{"objectId":'${O2}'},{"objectId":'${O3}'},{"objectId":'${O4}'},{"objectId":'${O5}'}]' "${BASE}/views/${VID}/objects"

# 6) Cleanup and save
run "DELETE /relations" curl_json -X DELETE -d '[{"id":"'${R1}'"},{"id":"'${R2}'"},{"id":"'${R3}'"},{"id":"'${R4}'"}]' "${BASE}/relations"
run "DELETE /elements" curl_json -X DELETE -d '[{"id":"'${E1}'"},{"id":"'${E2}'"},{"id":"'${E3}'"},{"id":"'${E4}'"},{"id":"'${E5}'"}]' "${BASE}/elements"
run "POST /model/save" curl_json -X POST "${BASE}/model/save"

echo "\n[SMOKE] Flow completed."

