#!/usr/bin/env bash
# Usage: ./scripts/get-token.sh [username] [password]
# Defaults: user / user123
#
# Requires: curl, jq
# Keycloak must be running on localhost:8082 (docker compose up keycloak)

set -euo pipefail

KC_URL="${KC_URL:-http://localhost:8082}"
REALM="descontovivo"
CLIENT_ID="descontovivo-cli"
USERNAME="${1:-user}"
PASSWORD="${2:-user123}"

RESPONSE=$(curl -s "${KC_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=${CLIENT_ID}" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}")

TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "ERROR: failed to obtain token for ${USERNAME}" >&2
  echo "$RESPONSE" | jq . >&2
  exit 1
fi

echo "$TOKEN"
