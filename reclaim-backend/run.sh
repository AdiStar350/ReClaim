#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$(dirname "$0")"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home}"
./mvnw spring-boot:run
