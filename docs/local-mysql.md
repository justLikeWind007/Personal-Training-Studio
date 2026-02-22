# Local MySQL Environment

## 1. Start database

```bash
docker compose up -d mysql
```

## 2. Run app with MySQL profile

```bash
SPRING_PROFILES_ACTIVE=mysql mvn -pl ptstudio-start spring-boot:run
```

Default database settings can be overridden with environment variables:

- `DB_HOST` (default `127.0.0.1`)
- `DB_PORT` (default `3306`)
- `DB_NAME` (default `ptstudio`)
- `DB_USER` (default `ptstudio`)
- `DB_PASSWORD` (default `ptstudio`)

## 3. Flyway migration

When `mysql` profile is active, Flyway runs `classpath:db/migration/V*.sql` at startup.

## 4. Test profile

`mvn test` uses `test` profile by default and keeps Flyway disabled to ensure fast and stable CI.

## 5. API smoke script

```bash
./scripts/smoke_api.sh
```

Optional env:

- `BASE_URL` (default `http://127.0.0.1:8080`)
- `TENANT_ID` (default `tenant-demo`)
- `STORE_ID` (default `store-001`)

## 6. MySQL Profile Smoke

```bash
./scripts/mysql_smoke.sh
```

脚本会自动：
- 启动 `docker compose` 的 MySQL
- 使用 `SPRING_PROFILES_ACTIVE=mysql` 启动服务
- 检查 `health` / `OpenAPI` / `settings` / `smoke_api.sh`

## 7. Reservation Outbox Event (mq + mysql)

如果需要把预约创建/取消事件写入 `t_outbox_event`，请启用组合 profile：

```bash
SPRING_PROFILES_ACTIVE=mq,mysql mvn -pl ptstudio-start spring-boot:run
```

可选环境变量：
- `PT_RESERVATION_EVENT_TOPIC`（默认 `ptstudio.reservation.changed`）
- `PT_RESERVATION_CREATED_TAG`（默认 `reservation_created`）
- `PT_RESERVATION_CANCELED_TAG`（默认 `reservation_canceled`）

## 8. Middleware Full Stack

如需同时启动 `mysql + redis + rocketmq` 联调环境，参考：

- `docs/local-middleware.md`
- `scripts/middleware_smoke.sh`
