#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_PID=""
UI_PID=""

# Keep Angular CLI startup non-interactive in scripted launches.
export NG_CLI_ANALYTICS=false

for command in java node npm; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Required command not found: $command" >&2
    exit 1
  fi
done

if [[ ! -x "$ROOT_DIR/mvnw" ]]; then
  echo "Maven wrapper is missing or not executable: $ROOT_DIR/mvnw" >&2
  exit 1
fi

if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "Installing frontend dependencies..."
  (cd "$ROOT_DIR/frontend" && npm ci)
fi

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM

  if [[ -n "$UI_PID" ]]; then
    kill "$UI_PID" 2>/dev/null || true
    wait "$UI_PID" 2>/dev/null || true
  fi
  if [[ -n "$API_PID" ]]; then
    kill "$API_PID" 2>/dev/null || true
    wait "$API_PID" 2>/dev/null || true
  fi

  exit "$exit_code"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

echo "Starting ARUS AI API at http://localhost:8080..."
(cd "$ROOT_DIR" && exec ./mvnw spring-boot:run) &
API_PID=$!

echo "Starting ARUS AI UI at http://localhost:4200..."
(cd "$ROOT_DIR/frontend" && exec npm start) &
UI_PID=$!

echo "Press Ctrl+C to stop the API and UI."
wait "$UI_PID"
