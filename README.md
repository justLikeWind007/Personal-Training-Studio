# PT Studio DDD4J

企业级私教工作室管理系统后端工程，基于 Java 17 + Spring Boot 3 + DDD 分层设计实现。仓库同时支持两种运行模式：

- 单体模式：`ptstudio-start` 一体化启动，适合本地开发和功能验证。
- 微服务模式：`gateway + biz-service + ops-service`，配合 Nacos/Sentinel 做服务治理与流控演练。

## 1. 技术栈

- Java 17
- Maven（多模块）
- Spring Boot 3.3.8
- Spring Cloud 2023.0.5
- Spring Cloud Alibaba 2023.0.3.3（Nacos / Sentinel）
- MyBatis-Plus 3.5.7
- MySQL 8.4 + Flyway
- Redis 7.4
- RocketMQ 5.3.3
- Elasticsearch 8.13.4 + Kibana
- SpringDoc OpenAPI

## 2. 模块结构

```text
ptstudio-ddd4j/
├── ptstudio-bom                 # 版本管理 BOM
├── ptstudio-dependencies        # 公共依赖聚合
├── ptstudio-common              # 跨域公共能力
├── ptstudio-core                # 核心领域（adapter/app/domain/infrastructure）
├── ptstudio-start               # 单体启动入口（默认端口 8080）
├── ptstudio-cloud-gateway       # 微服务网关（默认端口 8080）
├── ptstudio-service-biz         # 业务服务（默认端口 8081）
├── ptstudio-service-ops         # 运营服务（默认端口 8082）
├── docs                         # 设计/运维/压测文档
└── scripts                      # 启停、冒烟、巡检、演练脚本
```

## 3. 环境要求

- JDK 17
- Maven 3.9+
- Docker / Docker Compose

## 4. 快速开始

### 4.1 单体模式（最小可运行）

默认使用 `test` profile（H2 内存库，Flyway 关闭）：

```bash
mvn -pl ptstudio-start spring-boot:run
```

服务启动后可验证：

- 健康检查：`http://127.0.0.1:8080/actuator/health`
- OpenAPI：`http://127.0.0.1:8080/v3/api-docs`
- Swagger UI：`http://127.0.0.1:8080/swagger-ui/index.html`

执行接口冒烟：

```bash
./scripts/smoke_api.sh
```

### 4.2 单体 + MySQL

```bash
docker compose up -d mysql
SPRING_PROFILES_ACTIVE=mysql mvn -pl ptstudio-start spring-boot:run
./scripts/mysql_smoke.sh
```

### 4.3 单体 + 完整中间件（mysql/redis/mq/es）

```bash
docker compose up -d mysql redis rocketmq-namesrv rocketmq-broker rocketmq-dashboard elasticsearch kibana nacos sentinel-dashboard
SPRING_PROFILES_ACTIVE=mysql,redis,mq,es mvn -pl ptstudio-start spring-boot:run
./scripts/middleware_smoke.sh
```

## 5. 微服务模式

一键启动（Nacos + gateway + biz + ops）：

```bash
./scripts/microservice_cluster_up.sh
```

冒烟验证：

```bash
./scripts/microservice_cluster_smoke.sh
```

停止集群：

```bash
./scripts/microservice_cluster_down.sh
```

网关路由规则：

- `/api/ops/**` -> `ptstudio-ops-service`
- `/api/**`、`/v3/api-docs/**`、`/swagger-ui/**` -> `ptstudio-biz-service`

## 6. 常用脚本

- `scripts/observability_smoke.sh`：可观测性冒烟（metrics / prometheus / dashboard）
- `scripts/release_precheck.sh`：发布前检查
- `scripts/enterprise_readiness_check.sh`：企业级一键巡检
- `scripts/security_baseline_check.sh`：安全基线巡检
- `scripts/nacos_sentinel_flow_rule.sh`：下发 Sentinel 流控规则
- `scripts/perf_gateway_baseline_and_limited.sh`：网关限流前后压测报告
- `scripts/failover_drill_redis_sentinel.sh`：Redis Sentinel 故障演练
- `scripts/failover_drill_es_cluster.sh`：Elasticsearch 集群故障演练
- `scripts/failover_drill_nacos_cluster.sh`：Nacos 集群故障演练
- `scripts/failover_drill_rocketmq_namesrv.sh`：RocketMQ NameServer 故障演练

## 7. 常用环境变量

- 数据库：`DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD`
- Redis：`REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD`
- RocketMQ：`ROCKETMQ_NAME_SERVER`
- ES：`ES_HOST` `ES_PORT` `ES_SCHEME`
- 服务治理：`NACOS_SERVER_ADDR` `SENTINEL_DASHBOARD` `SENTINEL_TRANSPORT_PORT`
- 租户上下文：`TENANT_ID` `STORE_ID`

## 8. 测试命令

全部测试：

```bash
mvn test
```

分层 CI 组：

```bash
mvn test -Pci-smoke
mvn test -Pci-security
mvn test -Pci-regression
```

## 9. 相关文档

- 微服务拆分说明：`docs/microservice-cluster.md`
- 本地中间件：`docs/local-middleware.md`
- 本地 MySQL：`docs/local-mysql.md`
- 中间件集群：`docs/local-middleware-cluster.md`
- 生产观测与告警基线：`docs/生产观测与告警基线_v1.md`
- 发布灰度与回滚：`docs/发布灰度与回滚Runbook_v1.md`
- 安全基线：`docs/安全基线与最小信任模型_v1.md`

