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

# Lombok 1.18.36 (Spring Boot 3.4.1) fails silently on JDK 24+,
# so always run on JDK 22 regardless of the shell's JAVA_HOME.
if JAVA_22_HOME="$(/usr/libexec/java_home -v 22 2>/dev/null)"; then
  export JAVA_HOME="$JAVA_22_HOME"
else
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home"
fi

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "error: no JDK 22 found at $JAVA_HOME — install JDK 22 (Lombok breaks on JDK 24+)" >&2
  exit 1
fi

if [[ -z "${FIREBASE_SERVICE_ACCOUNT:-}" ]]; then
  echo "warning: FIREBASE_SERVICE_ACCOUNT is not set; push notifications will be disabled" >&2
elif [[ ! -f "$FIREBASE_SERVICE_ACCOUNT" ]]; then
  echo "warning: FIREBASE_SERVICE_ACCOUNT points to a missing file ($FIREBASE_SERVICE_ACCOUNT); push notifications will be disabled" >&2
fi

exec ./mvnw spring-boot:run
