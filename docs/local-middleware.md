# Local Middleware Environment (v2)

## 1. Start middleware stack

```bash
docker compose up -d mysql redis rocketmq-namesrv rocketmq-broker rocketmq-dashboard
```

Services:
- MySQL: `127.0.0.1:3306`
- Redis: `127.0.0.1:6379`
- RocketMQ NameServer: `127.0.0.1:9876`
- RocketMQ Broker: `127.0.0.1:10911`
- RocketMQ Dashboard: `http://127.0.0.1:8088`

## 2. Middleware smoke check

```bash
./scripts/middleware_smoke.sh
```

## 3. Outbox retry/dead-letter drill

```bash
./scripts/outbox_compensation_drill.sh
```

脚本会自动：
- 启动 `mq,mysql` profile 服务
- 写入一个 `drill_fail` 事件到 `t_outbox_event`
- 等待重试并验证事件进入 `DEAD`
- 日志输出到 `/tmp/ptstudio_outbox_drill.log`

## 4. Run app with combined profiles

```bash
SPRING_PROFILES_ACTIVE=mysql,redis,mq mvn -pl ptstudio-start spring-boot:run
```

## 5. Environment variables

- MySQL:
  - `DB_HOST` (default `127.0.0.1`)
  - `DB_PORT` (default `3306`)
  - `DB_NAME` (default `ptstudio`)
  - `DB_USER` (default `ptstudio`)
  - `DB_PASSWORD` (default `ptstudio`)
- Redis:
  - `REDIS_HOST` (default `127.0.0.1`)
  - `REDIS_PORT` (default `6379`)
  - `REDIS_PASSWORD` (default empty)
- MQ:
  - `ROCKETMQ_NAME_SERVER` (default `127.0.0.1:9876`)
  - `PT_RESERVATION_EVENT_TOPIC` (default `ptstudio.reservation.changed`)
  - `PT_RESERVATION_CREATED_TAG` (default `reservation_created`)
  - `PT_RESERVATION_CANCELED_TAG` (default `reservation_canceled`)
  - `PT_CONSUMPTION_EVENT_TOPIC` (default `ptstudio.consumption.changed`)
  - `PT_CONSUMPTION_CONSUMED_TAG` (default `consumption_consumed`)
  - `PT_CONSUMPTION_REVERSED_TAG` (default `consumption_reversed`)
  - `PT_OUTBOX_DISPATCH_INTERVAL_MS` (default `5000`)
  - `PT_OUTBOX_BATCH_SIZE` (default `50`)
  - `PT_OUTBOX_MAX_RETRY` (default `5`)
  - `PT_OUTBOX_RETRY_DELAY_SECONDS` (default `30`)
  - `PT_OUTBOX_FAIL_TAGS` (default empty)

## 6. Notes

- 当前代码中 `mq+mysql` 已用于预约事件 outbox 入库，不依赖 RocketMQ SDK 即可运行。
- RocketMQ 容器用于 v2 异步投递链路联调与后续演进。
