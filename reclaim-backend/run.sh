#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$(dirname "$0")"

# Load shared config: MONGODB_URI, JWT_SECRET, FIREBASE_SERVICE_ACCOUNT…
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

# I look for JDK 25 (LTS). 22 is gone from most downloads.
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA_25_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null)"; then
    export JAVA_HOME="$JAVA_25_HOME"
  fi
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "error: no JDK 25 found. Install Temurin 25 and set JAVA_HOME." >&2
  exit 1
fi

# Relative paths in .env are from the repo root, not this folder.
if [[ -n "${FIREBASE_SERVICE_ACCOUNT:-}" && "$FIREBASE_SERVICE_ACCOUNT" != /* ]]; then
  FIREBASE_SERVICE_ACCOUNT="$ROOT_DIR/$FIREBASE_SERVICE_ACCOUNT"
  export FIREBASE_SERVICE_ACCOUNT
fi

if [[ -z "${FIREBASE_SERVICE_ACCOUNT:-}" ]]; then
  echo "warning: FIREBASE_SERVICE_ACCOUNT is not set; push notifications will be disabled" >&2
elif [[ ! -f "$FIREBASE_SERVICE_ACCOUNT" ]]; then
  echo "warning: FIREBASE_SERVICE_ACCOUNT points to a missing file ($FIREBASE_SERVICE_ACCOUNT); push notifications will be disabled" >&2
fi

exec ./mvnw spring-boot:run
