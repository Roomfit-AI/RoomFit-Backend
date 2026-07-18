#!/usr/bin/env bash
set -euo pipefail

# Safe-by-default deployment verification. It never targets a production URL
# implicitly and intentionally leaves resources behind because deleting a Room
# can leave historical Layout snapshots without a database foreign key.
: "${ROOMFIT_BASE_URL:?Set ROOMFIT_BASE_URL (for example https://api.example.com)}"

BASE_URL="${ROOMFIT_BASE_URL%/}"
CLIENT_A="${ROOMFIT_CLIENT_A:-$(uuidgen | tr '[:upper:]' '[:lower:]')}"
CLIENT_B="${ROOMFIT_CLIENT_B:-$(uuidgen | tr '[:upper:]' '[:lower:]')}"
SMOKE_SUFFIX="$(date -u +%Y%m%dT%H%M%SZ)-$$"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

LAST_BODY=""
LAST_STATUS=""

require_command() {
  command -v "$1" >/dev/null 2>&1 || { echo "Missing required command: $1" >&2; exit 1; }
}

json_get() {
  python3 - "$1" "$2" <<'PY'
import json, sys
value = json.load(open(sys.argv[1], encoding="utf-8"))
for part in sys.argv[2].split("."):
    value = value[part]
print(value)
PY
}

assert_json() {
  python3 - "$1" "$2" "$3" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
kind, expected = sys.argv[2:]
if kind == "room_list":
    ids = {str(item["roomId"]) for item in data["data"]}
    wanted, unwanted = expected.split(":")
    ok = wanted in ids and unwanted not in ids
elif kind == "success":
    ok = data.get("success") is True
elif kind == "same_data":
    other = json.load(open(expected, encoding="utf-8"))
    ok = data.get("data") == other.get("data")
else:
    raise SystemExit("Unknown assertion")
if not ok:
    print(json.dumps(data, ensure_ascii=False, indent=2), file=sys.stderr)
    raise SystemExit("JSON assertion failed: " + kind)
PY
}

call() {
  local client="$1" method="$2" path="$3" body="${4:-}"
  LAST_BODY="$TMP_DIR/response-$(date +%s%N).json"
  local args=(-sS -o "$LAST_BODY" -w "%{http_code}" -X "$method" "$BASE_URL$path"
    -H "X-RoomFit-Client-Id: $client")
  if [[ -n "$body" ]]; then
    args+=(-H "Content-Type: application/json" -d "$body")
  fi
  LAST_STATUS="$(curl "${args[@]}")"
}

call_public() {
  LAST_BODY="$TMP_DIR/response-$(date +%s%N).json"
  LAST_STATUS="$(curl -sS -o "$LAST_BODY" -w "%{http_code}" "$BASE_URL$1")"
}

expect_status() {
  local expected="$1" label="$2"
  if [[ "$LAST_STATUS" != "$expected" ]]; then
    echo "FAIL $label: expected HTTP $expected, got $LAST_STATUS" >&2
    cat "$LAST_BODY" >&2
    exit 1
  fi
  echo "OK   $label"
}

require_command curl
require_command python3
require_command uuidgen

echo "RoomFit deployment smoke test"
echo "Base URL: $BASE_URL"
echo "Client A/B: ${CLIENT_A:0:8}… / ${CLIENT_B:0:8}…"

call_public "/health"
expect_status 200 "health"

upload_body() {
  printf '{"name":"deployment-smoke-%s-%s","room":{"width":4.5,"depth":4.5,"height":2.5,"unit":"meter"},"openings":[],"furniture":[]}' "$1" "$SMOKE_SUFFIX"
}

call "$CLIENT_A" POST "/api/rooms/upload" "$(upload_body A)"
expect_status 201 "client A room upload"
ROOM_A="$(json_get "$LAST_BODY" "data.roomId")"

call "$CLIENT_B" POST "/api/rooms/upload" "$(upload_body B)"
expect_status 201 "client B room upload"
ROOM_B="$(json_get "$LAST_BODY" "data.roomId")"
[[ "$ROOM_A" != "$ROOM_B" ]] || { echo "Room IDs must differ" >&2; exit 1; }

call "$CLIENT_A" GET "/api/rooms/uploads/recent"
expect_status 200 "client A room list"
assert_json "$LAST_BODY" room_list "$ROOM_A:$ROOM_B"
call "$CLIENT_B" GET "/api/rooms/uploads/recent"
expect_status 200 "client B room list"
assert_json "$LAST_BODY" room_list "$ROOM_B:$ROOM_A"

call "$CLIENT_B" GET "/api/rooms/$ROOM_A"
expect_status 404 "cross-client room read is hidden"

call "$CLIENT_B" GET "/api/rooms/$ROOM_B"
expect_status 200 "client B room baseline"
ROOM_B_BASELINE="$LAST_BODY"

CONTEXT_BODY="{\"roomId\":$ROOM_A,\"lifestyleGoal\":\"STUDY_FOCUSED\",\"designStyle\":[\"MINIMAL\"],\"requiredItems\":[\"desk\"],\"optionalItems\":[],\"selectedImageIds\":[1],\"selectedProductIds\":[]}"
call "$CLIENT_A" POST "/api/agent/context" "$CONTEXT_BODY"
expect_status 201 "client A context"
CONTEXT_A="$(json_get "$LAST_BODY" "data.contextId")"

call "$CLIENT_A" POST "/api/layouts/recommend" "{\"roomId\":$ROOM_A,\"contextId\":$CONTEXT_A}"
expect_status 201 "recommendation"
assert_json "$LAST_BODY" success ignored
LAYOUT_A="$(json_get "$LAST_BODY" "data.layoutId")"

call "$CLIENT_B" GET "/api/layouts/$LAYOUT_A"
expect_status 404 "cross-client layout read is hidden"
call "$CLIENT_B" POST "/api/layouts/$LAYOUT_A/confirm"
expect_status 404 "cross-client confirm is blocked"

call "$CLIENT_A" GET "/api/layouts/$LAYOUT_A"
expect_status 200 "client A layout reload"
call "$CLIENT_A" POST "/api/layouts/$LAYOUT_A/confirm"
expect_status 200 "client A confirm"

call "$CLIENT_B" GET "/api/rooms/$ROOM_B"
expect_status 200 "client B room reload"
assert_json "$LAST_BODY" same_data "$ROOM_B_BASELINE"

echo
echo "Smoke resources retained for inspection (safe cleanup is not automatic):"
echo "  client A roomId=$ROOM_A layoutId=$LAYOUT_A"
echo "  client B roomId=$ROOM_B"
