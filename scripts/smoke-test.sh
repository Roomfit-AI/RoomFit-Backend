#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-${BASE_URL:-${BASE:-https://roomfit-backend.onrender.com}}}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

LAST_BODY=""
LAST_STATUS=""

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "❌ Required command not found: $1"
    exit 1
  fi
}

json_get() {
  local file="$1"
  local path="$2"

  python3 - "$file" "$path" <<'PY'
import json
import sys

file_path = sys.argv[1]
json_path = sys.argv[2].split(".")

with open(file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

current = data
for key in json_path:
    current = current[key]

print(current)
PY
}

json_get_optional() {
  local file="$1"
  local path="$2"
  local default_value="${3:--}"

  python3 - "$file" "$path" "$default_value" <<'PY'
import json
import sys

file_path = sys.argv[1]
json_path = sys.argv[2].split(".")
default_value = sys.argv[3]

with open(file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

current = data
for key in json_path:
    if not isinstance(current, dict):
        current = default_value
        break
    current = current.get(key, default_value)

print(current)
PY
}

validate_feedback_response() {
  local file="$1"
  local room_width="$2"
  local room_depth="$3"

  python3 - "$file" "$room_width" "$room_depth" <<'PY'
import json
import math
import sys

with open(sys.argv[1], "r", encoding="utf-8") as f:
    response = json.load(f)

room_width = float(sys.argv[2])
room_depth = float(sys.argv[3])

def fail(message):
    print(f"❌ {message}", file=sys.stderr)
    sys.exit(1)

if response.get("success") is not True:
    fail("Feedback response success must be true")

data = response.get("data")
if not isinstance(data, dict):
    fail("Feedback response data is missing")

furniture = data.get("recommendedFurniture")
if not isinstance(furniture, list):
    fail("recommendedFurniture is missing")

desk = next((item for item in furniture if item.get("id") == "desk-1"), None)
if not isinstance(desk, dict):
    fail("desk-1 is missing from recommendedFurniture")

width = desk.get("width")
depth = desk.get("depth")
rotation = desk.get("rotation")
position = desk.get("position")
if not isinstance(width, (int, float)) or width < 1.4:
    fail("desk-1 width must be at least 1.4")
if not isinstance(depth, (int, float)) or not isinstance(rotation, (int, float)):
    fail("desk-1 depth or rotation is missing")
if not isinstance(position, dict) or not all(
        isinstance(position.get(axis), (int, float)) for axis in ("x", "z")):
    fail("desk-1 position is missing")

normalized_rotation = rotation % 360.0
nearest_right_angle = round(normalized_rotation / 90.0) * 90.0
if abs(normalized_rotation - nearest_right_angle) <= 1.0e-4:
    right_angle = round(nearest_right_angle) % 360
    if right_angle in (90, 270):
        effective_width, effective_depth = depth, width
    else:
        effective_width, effective_depth = width, depth
else:
    radians = math.radians(normalized_rotation)
    absolute_cosine = abs(math.cos(radians))
    absolute_sine = abs(math.sin(radians))
    effective_width = width * absolute_cosine + depth * absolute_sine
    effective_depth = width * absolute_sine + depth * absolute_cosine

epsilon = 1.0e-6
if (position["x"] < effective_width / 2.0 - epsilon
        or position["x"] > room_width - effective_width / 2.0 + epsilon):
    fail("desk-1 position.x is outside the rotation-aware room boundary")
if (position["z"] < effective_depth / 2.0 - epsilon
        or position["z"] > room_depth - effective_depth / 2.0 + epsilon):
    fail("desk-1 position.z is outside the rotation-aware room boundary")

validation = data.get("validationResult")
if not isinstance(validation, dict) or validation.get("boundaryValid") is not True:
    fail("validationResult.boundaryValid must be true")

print(f"   desk-1.width={width}")
print(f"   desk-1.position.x={position['x']}")
print(f"   desk-1.position.z={position['z']}")
print(f"   desk-1.rotation={rotation}")
print("   validationResult.boundaryValid=true")
PY
}

call_api() {
  local method="$1"
  local path="$2"
  local body="${3:-}"

  LAST_BODY="$TMP_DIR/response_$(date +%s%N).json"

  if [[ -n "$body" ]]; then
    LAST_STATUS="$(
      curl -sS -o "$LAST_BODY" -w "%{http_code}" \
        -X "$method" "$BASE_URL$path" \
        -H "Content-Type: application/json" \
        -d "$body"
    )"
  else
    LAST_STATUS="$(
      curl -sS -o "$LAST_BODY" -w "%{http_code}" \
        -X "$method" "$BASE_URL$path"
    )"
  fi
}

expect_status() {
  local expected="$1"
  local label="$2"

  if [[ "$LAST_STATUS" != "$expected" ]]; then
    echo "❌ $label failed"
    echo "Expected HTTP $expected, got HTTP $LAST_STATUS"
    echo "Response:"
    cat "$LAST_BODY"
    echo
    exit 1
  fi

  echo "✅ $label"
}

require_command curl
require_command python3

echo "RoomFit Backend Smoke Test"
echo "BASE_URL=$BASE_URL"
echo

call_api GET "/health"
expect_status 200 "GET /health"

call_api GET "/api/products/mock"
expect_status 200 "GET /api/products/mock"

call_api GET "/api/styles/images"
expect_status 200 "GET /api/styles/images"

call_api GET "/api/rooms/1"
expect_status 200 "GET /api/rooms/1"

ROOM_WIDTH="$(json_get "$LAST_BODY" "data.room.width")"
ROOM_DEPTH="$(json_get "$LAST_BODY" "data.room.depth")"

CONTEXT_REQUEST='{
  "roomId": 1,
  "lifestyleGoal": "STUDY_FOCUSED",
  "designStyle": ["MINIMAL", "WHITE_TONE"],
  "requiredItems": ["bed", "desk", "chair"],
  "optionalItems": ["storage", "rug", "lamp"],
  "selectedImageIds": [1, 3],
  "selectedProductIds": ["desk-01", "chair-01", "lamp-01"]
}'

call_api POST "/api/agent/context" "$CONTEXT_REQUEST"
expect_status 201 "POST /api/agent/context"

CONTEXT_ID="$(json_get "$LAST_BODY" "data.contextId")"
echo "   contextId=$CONTEXT_ID"

RECOMMEND_REQUEST="{
  \"roomId\": 1,
  \"contextId\": $CONTEXT_ID
}"

call_api POST "/api/layouts/recommend" "$RECOMMEND_REQUEST"
expect_status 201 "POST /api/layouts/recommend"

LAYOUT_ID="$(json_get "$LAST_BODY" "data.layoutId")"
echo "   layoutId=$LAYOUT_ID"

FEEDBACK_REQUEST="{
  \"layoutId\": $LAYOUT_ID,
  \"feedback\": \"책상을 조금 더 넓게 쓰고 싶어\"
}"

call_api POST "/api/layouts/feedback" "$FEEDBACK_REQUEST"
expect_status 201 "POST /api/layouts/feedback"

if ! validate_feedback_response "$LAST_BODY" "$ROOM_WIDTH" "$ROOM_DEPTH"; then
  echo "Response:"
  cat "$LAST_BODY"
  echo
  exit 1
fi

INTENT_SOURCE="$(json_get_optional "$LAST_BODY" "data.interpretedIntent.source")"
FALLBACK_USED="$(json_get_optional "$LAST_BODY" "data.interpretedIntent.fallbackUsed")"
TARGET_FURNITURE="$(json_get_optional "$LAST_BODY" "data.interpretedIntent.targetFurniture")"

echo "   interpretedIntent.source=$INTENT_SOURCE"
echo "   interpretedIntent.targetFurniture=$TARGET_FURNITURE"
echo "   interpretedIntent.fallbackUsed=$FALLBACK_USED"

if [[ "$INTENT_SOURCE" != "-" && "$INTENT_SOURCE" != "LLM" && "$INTENT_SOURCE" != "RULE_BASED" ]]; then
  echo "❌ Unsupported feedback intent source: $INTENT_SOURCE"
  echo "Response:"
  cat "$LAST_BODY"
  echo
  exit 1
fi

echo
echo "🎉 Smoke test passed"
