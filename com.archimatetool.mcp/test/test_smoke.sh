#!/usr/bin/env bash
set -euo pipefail

# Extended smoke test for Archi MCP Plugin (localhost only)
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
curl -sS "${BASE}/views/${VID1}/image?format=png&scale=1.0&bg=transparent&margin=0" -o /tmp/view.png -D /tmp/headers.txt > /dev/null
grep -iq 'image/png' /tmp/headers.txt
[ -s /tmp/view.png ]
code=$(curl -s -o /dev/null -w "%{http_code}" "${BASE}/views/${VID1}/image?format=svg")
if [ "$code" != "400" ]; then echo "Unexpected SVG response code: $code"; exit 1; fi

# 10) Search
run "GET /search?q=Actor" curl_json "${BASE}/search?q=Actor"
run "GET /search with params" curl_json "${BASE}/search?q=Actor&kind=element&limit=1&offset=0&debug=true"

# 11) Save model
run "POST /model/save" curl_json -X POST "${BASE}/model/save"

# 12) Script stubs
curl -s -o /dev/null -w "%{http_code}" "${BASE}/script/engines" | grep -q '^501$'
curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE}/script/run" | grep -q '^501$'

# 13) Error checks
curl -s -o /dev/null -w "%{http_code}" "${BASE}/elements/bogus" | grep -q '^404$'
curl -s -o /dev/null -w "%{http_code}" "${BASE}/views/bogus" | grep -q '^404$'
curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE}/status" | grep -q '^405$'

# 14) Cleanup
curl -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/relations/${REL}" | grep -q '^204$'
curl -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/elements/${E1}" | grep -q '^204$'
curl -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/elements/${E2}" | grep -q '^204$'
curl -s -o /dev/null -w "%{http_code}" -X DELETE "${BASE}/views/${VID1}" | grep -q '^204$'

echo "\n[SMOKE] Flow completed."
