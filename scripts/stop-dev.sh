#!/usr/bin/env bash
set -euo pipefail

APP_NAME="events"
KAFKA_CONTAINER_NAME="dev-kafka"
NETWORK_NAME="events-net"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
log() { echo -e "[stop-dev] $*"; }

# --- stop app instances ---
if [[ -d "$ROOT_DIR/.pids" ]]; then
  for f in "$ROOT_DIR/.pids"/${APP_NAME}-*.pid; do
    [[ -e "$f" ]] || continue
    PID=$(cat "$f" || true)
    if [[ -n "${PID:-}" ]] && ps -p "$PID" >/dev/null 2>&1; then
      log "Killing $f (pid=$PID)"
      kill "$PID" || true
    fi
    rm -f "$f"
  done
else
  log "No PID directory found; skipping app stop"
fi

# --- stop & remove kafka ---
if docker ps -a --format '{{.Names}}' | grep -q "^$KAFKA_CONTAINER_NAME$"; then
  log "Stopping Kafka container $KAFKA_CONTAINER_NAME"
  docker rm -f "$KAFKA_CONTAINER_NAME" >/dev/null || true
fi

# --- remove network (if empty) ---
if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  # Attempt removal; will succeed only if unused
  docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
fi

log "Teardown complete."
