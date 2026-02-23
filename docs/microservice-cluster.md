# Microservice Cluster 改造说明（v5）

## 1. 拆分目标

在保留原 `ptstudio-start` 单体启动能力的前提下，新增可并行演进的微服务集群：
- 服务注册中心：`Nacos`（Spring Cloud Alibaba）
- API 网关：`ptstudio-cloud-gateway`
- 业务服务：`ptstudio-biz-service`
- 运营服务：`ptstudio-ops-service`

## 2. 路由与职责

- 网关路由：
  - `/api/ops/**` -> `ptstudio-ops-service`
  - `/api/**`、`/v3/api-docs/**`、`/swagger-ui/**` -> `ptstudio-biz-service`
- 控制器拆分：
  - `ptstudio-biz-service` 排除 `core.adapter.ops` 控制器
  - `ptstudio-ops-service` 仅保留 `core.adapter.ops` 控制器
- 流控组件：
  - 已接入 Spring Cloud Alibaba Sentinel（gateway/biz/ops）
  - 网关与 ops 服务的流控规则存储在 Nacos `SENTINEL_GROUP`
  - 网关限流返回统一 JSON（`code=GATEWAY_FLOW_LIMITED`）

## 3. 本地启动

```bash
./scripts/microservice_cluster_up.sh
```

脚本会自动拉起 `docker compose` 的 `nacos` 容器并等待健康检查。

停止：

```bash
./scripts/microservice_cluster_down.sh
```

冒烟：

```bash
./scripts/microservice_cluster_smoke.sh
```

可观测性巡检：

```bash
./scripts/observability_smoke.sh
```

发布前检查：

```bash
./scripts/release_precheck.sh
```

企业级一键巡检：

```bash
./scripts/enterprise_readiness_check.sh
```

安全基线巡检：

```bash
./scripts/security_baseline_check.sh
```

中间件集群拓扑（开发/测试）：

```bash
docker compose -f docker-compose.cluster.yml up -d
./scripts/middleware_cluster_smoke.sh
```

集群说明文档：`docs/local-middleware-cluster.md`
观测基线文档：`docs/生产观测与告警基线_v1.md`
告警规则模板：`observability/prometheus-alert-rules.yml`
发布与回滚 runbook：`docs/发布灰度与回滚Runbook_v1.md`
发布检查清单：`docs/发布检查清单_v1.md`
故障演练手册：`docs/故障演练手册_v1.md`
Redis 演练脚本：`scripts/failover_drill_redis_sentinel.sh`
ES 演练脚本：`scripts/failover_drill_es_cluster.sh`
Nacos 演练脚本：`scripts/failover_drill_nacos_cluster.sh`
RocketMQ 演练脚本：`scripts/failover_drill_rocketmq_namesrv.sh`
安全基线文档：`docs/安全基线与最小信任模型_v1.md`
安全巡检清单：`docs/安全巡检清单_v1.md`
中间件生产参数基线：`docs/中间件生产参数基线_v1.md`

## 4. 高并发链路测试

测试链路：`Gateway -> Ops Service -> Async Queue`

压测前可先下发限流规则：

```bash
./scripts/nacos_sentinel_flow_rule.sh
```

一键生成“限流前后对比报告”：

```bash
./scripts/perf_gateway_baseline_and_limited.sh
```

```bash
python3 scripts/perf_gateway_concurrency.py
```

生成压测文字报告（可归档）：

```bash
python3 scripts/perf_chain_text_report.py \
  --baseline /tmp/ptstudio-perf/gateway_baseline.json \
  --limited /tmp/ptstudio-perf/gateway_limited.json \
  --output docs/链路高并发压测报告_v1.md \
  --env local-cluster \
  --operator your-name
```

输出指标：
- 总请求数、并发度
- 成功/限流/失败数
- 平均延迟、P95、最大延迟
- 吞吐（RPS）
- 队列消费闭环结果
- 报告脚本输出 `gateway_compare_report.md`
- 文字报告输出 `docs/链路高并发压测报告_v1.md`
- 故障注入报告输出 `docs/故障注入压测报告_v1.md`
- 长稳压测报告输出 `docs/长稳压测报告_v1.md`

压测治理手册：`docs/高并发压测治理手册_v1.md`

## 5. 当前约束

- 受当前环境网络策略限制，无法在线拉取新的 Maven 依赖并做完整编译。
- 代码已按 Spring Cloud Alibaba 标准结构完成改造，建议在可访问 Maven 仓库的环境执行完整构建与压测。
