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

## 3. Run app with combined profiles

```bash
SPRING_PROFILES_ACTIVE=mysql,redis,mq mvn -pl ptstudio-start spring-boot:run
```

## 4. Environment variables

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

## 5. Notes

- 当前代码中 `mq+mysql` 已用于预约事件 outbox 入库，不依赖 RocketMQ SDK 即可运行。
- RocketMQ 容器用于 v2 异步投递链路联调与后续演进。
