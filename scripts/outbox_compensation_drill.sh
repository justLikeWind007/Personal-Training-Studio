#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DRILL_EVENT_ID="drill_$(date +%s)"
DB_NAME="${DB_NAME:-ptstudio}"
DB_USER="${DB_USER:-ptstudio}"
DB_PASSWORD="${DB_PASSWORD:-ptstudio}"
MAX_RETRY="${PT_OUTBOX_MAX_RETRY:-3}"

echo "[1/6] 启动中间件 (mysql/redis/rocketmq)"
docker compose up -d mysql redis rocketmq-namesrv rocketmq-broker >/dev/null

echo "[2/6] 启动服务 (mq,mysql) 并注入失败tag"
PT_OUTBOX_FAIL_TAGS=drill_fail PT_OUTBOX_MAX_RETRY="$MAX_RETRY" SPRING_PROFILES_ACTIVE=mq,mysql mvn -q -pl ptstudio-start spring-boot:run >/tmp/ptstudio_outbox_drill.log 2>&1 &
APP_PID=$!
trap 'kill $APP_PID >/dev/null 2>&1 || true' EXIT

for i in {1..50}; do
  if curl -sS --max-time 2 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
    break
  fi
  sleep 2
done

echo "[3/6] 写入演练事件到 outbox"
docker exec -i ptstudio-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<SQL
INSERT INTO t_outbox_event(tenant_id, store_id, event_id, topic, tag, biz_type, biz_id, payload_json, status, retry_count)
VALUES(1, 1, '$DRILL_EVENT_ID', 'ptstudio.drill.changed', 'drill_fail', 'DRILL', 1, JSON_OBJECT('eventType','DRILL_FAIL'), 'NEW', 0);
SQL

echo "[4/6] 轮询事件状态，期望进入 DEAD"
status=""
retry_count=0
for i in {1..60}; do
  row=$(docker exec -i ptstudio-mysql mysql -N -B -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" \
    -e "SELECT status,retry_count FROM t_outbox_event WHERE event_id='$DRILL_EVENT_ID' LIMIT 1;")
  status=$(echo "$row" | awk '{print $1}')
  retry_count=$(echo "$row" | awk '{print $2}')
  echo "  poll=$i status=${status:-N/A} retry=${retry_count:-0}"
  if [[ "${status:-}" == "DEAD" ]]; then
    break
  fi
  sleep 2
done

echo "[5/6] 校验演练结果"
if [[ "${status:-}" != "DEAD" ]]; then
  echo "演练失败：事件未进入 DEAD，当前状态=${status:-N/A}"
  echo "应用日志：/tmp/ptstudio_outbox_drill.log"
  exit 1
fi

echo "[6/6] 演练通过"
echo "event_id=$DRILL_EVENT_ID status=$status retry_count=$retry_count"
