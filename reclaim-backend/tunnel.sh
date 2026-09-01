#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────
#  ReClaim — public tunnel for the backend
# ─────────────────────────────────────────────────────────────────────────
# Exposes the locally-running Spring Boot server (port 8080) on a STABLE
# public URL so the Android app can reach it from any network (other Wi-Fi,
# cellular, etc.) without editing the app's BASE_URL each time.
#
# One-time setup:
#   1. Sign in to https://dashboard.ngrok.com
#   2. Reserve your free static domain at
#      https://dashboard.ngrok.com/domains  (looks like  foo-bar.ngrok-free.app)
#   3. Put it below (or export NGROK_DOMAIN before running this script).
#   4. Set that same domain (with https:// and a trailing slash) as BASE_URL in
#      reclaim-project/.../network/RetrofitClient.java
#
# Usage:
#   ./tunnel.sh                 # uses NGROK_DOMAIN or the default below
#   NGROK_DOMAIN=foo.ngrok-free.app ./tunnel.sh
#
# Run this ALONGSIDE the backend (start the server with ./run.sh first).
# ─────────────────────────────────────────────────────────────────────────
set -euo pipefail

# Reserved ngrok static domain (no https://, no slash).
DEFAULT_DOMAIN="decal-magnetism-coherent.ngrok-free.dev"

DOMAIN="${NGROK_DOMAIN:-$DEFAULT_DOMAIN}"
PORT="${PORT:-8080}"

if [[ "$DOMAIN" == "YOUR-DOMAIN.ngrok-free.app" ]]; then
  echo "ERROR: No ngrok domain set."
  echo "  Reserve a free static domain at https://dashboard.ngrok.com/domains"
  echo "  then edit DEFAULT_DOMAIN in this script or run:"
  echo "    NGROK_DOMAIN=your-name.ngrok-free.app ./tunnel.sh"
  exit 1
fi

if ! command -v ngrok >/dev/null 2>&1; then
  echo "ERROR: ngrok is not installed. Install with: brew install ngrok"
  exit 1
fi

echo "Starting ngrok tunnel:"
echo "  https://$DOMAIN  ->  http://localhost:$PORT"
echo "Set RetrofitClient.BASE_URL to: https://$DOMAIN/"
echo
exec ngrok http --url="$DOMAIN" "$PORT"
