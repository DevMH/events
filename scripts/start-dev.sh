#!/usr/bin/env bash
set -euo pipefail

# --- config ---
APP_NAME="events"
BOOTSTRAP="localhost:9092"
KAFKA_CONTAINER_NAME="dev-kafka"
NETWORK_NAME="events-net"
PORT1=8081
PORT2=8082

# --- helpers ---
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR=""
log() { echo -e "[start-dev] $*"; }

# --- ensure docker ---
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required" >&2; exit 1
fi

# --- build app jar ---
log "Building Spring Boot jar (skipping tests)…"
(cd "$ROOT_DIR" && ./gradlew -x test bootJar)
JAR=$(ls "$ROOT_DIR"/build/libs/*-SNAPSHOT.jar | head -n 1)
if [[ -z "$JAR" ]]; then echo "Jar not found in build/libs" >&2; exit 1; fi
log "Using JAR: $JAR"

# --- docker network ---
if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  log "Creating docker network $NETWORK_NAME"
  docker network create "$NETWORK_NAME"
fi

# --- start kafka (single-node Kraft) ---
if docker ps -a --format '{{.Names}}' | grep -q "^$KAFKA_CONTAINER_NAME$"; then
  log "Kafka container already exists; (re)starting"
  docker start "$KAFKA_CONTAINER_NAME" >/dev/null || true
else
  log "Starting Kafka container $KAFKA_CONTAINER_NAME on $BOOTSTRAP"
  docker run -d --name "$KAFKA_CONTAINER_NAME" \
    --network "$NETWORK_NAME" \
    -p 9092:9092 \
    -e KAFKA_CFG_NODE_ID=1 \
    -e KAFKA_CFG_PROCESS_ROLES=broker,controller \
    -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
    -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
    -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_CFG_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
    bitnami/kafka:3.7 >/dev/null
fi

# --- wait for kafka port ---
log "Waiting for Kafka to accept connections at $BOOTSTRAP…"
for i in {1..60}; do
  if (echo >/dev/tcp/127.0.0.1/9092) >/dev/null 2>&1; then break; fi
  sleep 1
  if [[ $i -eq 60 ]]; then echo "Kafka did not start on 9092" >&2; exit 1; fi
done
log "Kafka is up."

# --- run two app instances ---
mkdir -p "$ROOT_DIR/.pids" "$ROOT_DIR/logs"

start_instance() {
  local port="$1"; shift
  local id="$1"; shift
  local logf="$ROOT_DIR/logs/${APP_NAME}-${id}.log"
  log "Starting $APP_NAME instance $id on port $port"
  APP_INSTANCE_ID="$id" \
  SPRING_KAFKA_BOOTSTRAP_SERVERS="$BOOTSTRAP" \
  SERVER_PORT="$port" \
  nohup java -jar "$JAR" \
    --server.port="$port" \
    --spring.kafka.bootstrap-servers="$BOOTSTRAP" \
    --app.instance-id="$id" \
    >"$logf" 2>&1 & echo $! > "$ROOT_DIR/.pids/${APP_NAME}-${id}.pid"
}

start_instance "$PORT1" "svc-1"
start_instance "$PORT2" "svc-2"

for t in case.updated case.created; do
  docker exec "$KAFKA_CONTAINER_NAME" bash -lc \
    "/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
     --create --if-not-exists --topic $t --partitions 1 --replication-factor 1"
done

log "Started. Logs in $ROOT_DIR/logs; PIDs in $ROOT_DIR/.pids"
log "Test endpoints (POST):"
log "  curl -X POST 'http://localhost:$PORT1/api/events/create/CASE-123'"
log "  curl -X POST 'http://localhost:$PORT2/api/events/create/CASE-234'"
