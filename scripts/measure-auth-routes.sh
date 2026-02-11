#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   SESSION_COOKIE="SESSION=<value>" ./scripts/measure-auth-routes.sh [image]
#   BASE_URL="http://127.0.0.1:8080" SESSION_COOKIE="SESSION=<value>" ./scripts/measure-auth-routes.sh
# Example:
#   SESSION_COOKIE="SESSION=abc123..." ./scripts/measure-auth-routes.sh mindlog:perf-v3

IMAGE="${1:-mindlog:perf-v3}"
NAME="bench-auth-routes"
PORT="${PORT:-38110}"
ENV_FILE="${ENV_FILE:-/Users/iggyu/git/mindlog/.env}"
SESSION_COOKIE="${SESSION_COOKIE:-}"
BASE_URL="${BASE_URL:-}"

if [[ -z "${SESSION_COOKIE}" ]]; then
  echo "SESSION_COOKIE is required. Example:"
  echo "  SESSION_COOKIE=\"SESSION=<value>\" ./scripts/measure-auth-routes.sh ${IMAGE}"
  exit 1
fi

ROUTES=(
  "/diaries"
  "/insights/emotions"
)

cleanup() {
  docker rm -f "${NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

if [[ -z "${BASE_URL}" ]]; then
  start_ms="$(python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
)"

  cleanup
  docker run -d --name "${NAME}" \
    --env-file "${ENV_FILE}" \
    -e SPRING_PROFILES_ACTIVE=prod \
    -p "${PORT}:8080" \
    "${IMAGE}" >/dev/null

  for _ in $(seq 1 240); do
    if curl -fsS "http://127.0.0.1:${PORT}/actuator/health/readiness" 2>/dev/null | rg -q '"status":"UP"'; then
      break
    fi
    sleep 0.5
  done

  end_ms="$(python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
)"
  BASE_URL="http://127.0.0.1:${PORT}"
  echo "image=${IMAGE} readiness_ms=$((end_ms - start_ms))"
else
  echo "base_url=${BASE_URL} (reuse-running-app)"
fi
echo "cookie_header=${SESSION_COOKIE%%=*}=***"

measure_pass() {
  local pass="$1"
  local non_ok_count=0
  echo "${pass}"
  for route in "${ROUTES[@]}"; do
    local result status
    result="$(curl -sS -o /dev/null \
      -H "Cookie: ${SESSION_COOKIE}" \
      -w "status=%{http_code} time=%{time_total}" \
      "${BASE_URL}${route}" || true)"
    echo "path=${route} ${result}"

    status="$(echo "${result}" | awk '{print $1}' | cut -d'=' -f2)"
    if [[ "${status}" != "200" ]]; then
      non_ok_count=$((non_ok_count + 1))
    fi
  done
  return "${non_ok_count}"
}

if ! measure_pass "first-pass"; then
  echo "WARN: non-200 status detected in first-pass."
  echo "      This usually means unauthenticated session (302 redirect to login)."
fi
if ! measure_pass "second-pass"; then
  echo "WARN: non-200 status detected in second-pass."
fi

echo "recent-slow-logs"
if [[ -z "${BASE_URL:-}" || "${BASE_URL}" == "http://127.0.0.1:${PORT}" ]]; then
  docker logs "${NAME}" --tail 200 2>&1 | rg "Slow method execution|HTTP access|WARMUP|AUTHZ_DENIED" || true
else
  echo "(skip docker logs: using external base_url)"
fi
