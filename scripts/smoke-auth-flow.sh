#!/usr/bin/env bash
# Smoke test: validates the complete authenticated flow end-to-end.
# Assumes containers are already running (docker compose up -d).
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_ID="$(date +%s)"

# --- Dependencies ---
for cmd in curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' is required but not installed." >&2
    exit 1
  fi
done

# --- Helpers ---
fail() {
  local step="$1" status="$2" body="$3"
  echo "[FAIL] $step"
  echo "  HTTP status: $status"
  echo "  Response: $body"
  exit 1
}

check_status() {
  local step="$1" expected="$2" actual="$3" body="$4"
  if [ "$actual" != "$expected" ]; then
    fail "$step" "$actual" "$body"
  fi
  echo "[OK] $step"
}

check_json() {
  local step="$1" expr="$2" expected="$3" body="$4"
  local actual
  actual=$(echo "$body" | jq -r "$expr")
  if [ "$actual" != "$expected" ]; then
    echo "[FAIL] $step — expected $expr = '$expected', got '$actual'"
    echo "  Response: $body"
    exit 1
  fi
}

http() {
  local method="$1" path="$2" token="${3:-}" data="${4:-}"
  local args=(-s -w "\n%{http_code}" -X "$method")
  args+=(-H "Content-Type: application/json")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-d "$data")
  curl "${args[@]}" "${API_URL}${path}"
}

parse() {
  local raw="$1"
  BODY=$(echo "$raw" | sed '$d')
  STATUS=$(echo "$raw" | tail -1)
}

# --- 1. Health ---
parse "$(http GET /api/v1/system/info)"
check_status "API health" "200" "$STATUS" "$BODY"

# --- 2. Obtain tokens ---
USER_TOKEN=$("$SCRIPT_DIR/get-token.sh" user user123)
MOD_TOKEN=$("$SCRIPT_DIR/get-token.sh" moderator moderator123)
echo "[OK] tokens obtained"

# --- 3. Authorization: user cannot access moderation ---
parse "$(http GET /api/v1/moderation/promotions "$USER_TOKEN")"
check_status "user cannot access moderation" "403" "$STATUS" "$BODY"

# --- 4. Authorization: moderator can access moderation ---
parse "$(http GET /api/v1/moderation/promotions "$MOD_TOKEN")"
check_status "moderator can access moderation" "200" "$STATUS" "$BODY"

# --- 5. Create promotion ---
PROMO_PAYLOAD=$(jq -n \
  --arg tid "$TEST_ID" \
  '{
    title: "Smoke auth flow \($tid)",
    url: "https://example.com/smoke-auth-flow-\($tid)",
    description: "Promoção criada pelo smoke test autenticado \($tid)",
    currentPrice: 10.00,
    imageUrl: "https://placehold.co/600x400",
    storeSlug: "amazon"
  }')

parse "$(http POST /api/v1/promotions "$USER_TOKEN" "$PROMO_PAYLOAD")"
check_status "promotion created" "201" "$STATUS" "$BODY"
check_json "promotion created" ".status" "PENDING_REVIEW" "$BODY"

PROMO_ID=$(echo "$BODY" | jq -r '.id')
PROMO_SLUG=$(echo "$BODY" | jq -r '.slug')

# --- 6. Approve promotion ---
APPROVE_PAYLOAD='{"action":"APPROVE","reason":"Smoke test authenticated flow"}'
parse "$(http PATCH "/api/v1/moderation/promotions/$PROMO_ID" "$MOD_TOKEN" "$APPROVE_PAYLOAD")"
check_status "promotion approved" "200" "$STATUS" "$BODY"
check_json "promotion approved" ".status" "PUBLISHED" "$BODY"

# --- 7. Public feed contains promotion ---
parse "$(http GET /api/v1/promotions)"
check_status "public feed" "200" "$STATUS" "$BODY"
if ! echo "$BODY" | jq -e ".content[] | select(.slug == \"$PROMO_SLUG\")" &>/dev/null; then
  fail "public feed contains promotion" "200" "$BODY"
fi
echo "[OK] public feed contains promotion"

# --- 8. Public detail ---
parse "$(http GET "/api/v1/promotions/$PROMO_SLUG")"
check_status "public detail" "200" "$STATUS" "$BODY"
check_json "public detail" ".status" "PUBLISHED" "$BODY"

# --- 9. Vote LIKE ---
parse "$(http PUT "/api/v1/promotions/$PROMO_SLUG/vote" "$USER_TOKEN" '{"type":"LIKE"}')"
check_status "vote registered" "200" "$STATUS" "$BODY"
check_json "vote registered" ".likesCount" "1" "$BODY"
check_json "vote registered" ".userVote" "LIKE" "$BODY"

# --- 10. Comment ---
COMMENT_PAYLOAD='{"authorName":"Smoke User","content":"Comentário criado pelo smoke test autenticado."}'
parse "$(http POST "/api/v1/promotions/$PROMO_SLUG/comments" "$USER_TOKEN" "$COMMENT_PAYLOAD")"
check_status "comment created" "201" "$STATUS" "$BODY"
check_json "comment created" ".removed" "false" "$BODY"
check_json "comment created" ".content" "Comentário criado pelo smoke test autenticado." "$BODY"

# --- 11. Final counters ---
parse "$(http GET "/api/v1/promotions/$PROMO_SLUG")"
check_json "final counters" ".likesCount" "1" "$BODY"
check_json "final counters" ".commentsCount" "1" "$BODY"
echo "[OK] final counters validated"

# --- 12. Public comments ---
parse "$(http GET "/api/v1/promotions/$PROMO_SLUG/comments")"
check_status "public comments" "200" "$STATUS" "$BODY"
if ! echo "$BODY" | jq -e '.[0].content' &>/dev/null; then
  fail "public comments contain entry" "200" "$BODY"
fi
echo "[OK] public comments listed"

echo ""
echo "=== All smoke tests passed ==="
