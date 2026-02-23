# Microservice Cluster 改造说明（v5）

## 1. 拆分目标

在保留原 `ptstudio-start` 单体启动能力的前提下，新增可并行演进的微服务集群：
- 服务注册中心：`ptstudio-cloud-registry`
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

## 3. 本地启动

```bash
./scripts/microservice_cluster_up.sh
```

停止：

```bash
./scripts/microservice_cluster_down.sh
```

冒烟：

```bash
./scripts/microservice_cluster_smoke.sh
```

## 4. 高并发链路测试

测试链路：`Gateway -> Ops Service -> Async Queue`

```bash
python3 scripts/perf_gateway_concurrency.py
```

输出指标：
- 总请求数、并发度
- 成功/失败数
- 平均延迟、P95、最大延迟
- 吞吐（RPS）
- 队列消费闭环结果

## 5. 当前约束

- 受当前环境网络策略限制，无法在线拉取新的 Maven 依赖并做完整编译。
- 代码已按 Spring Cloud 标准结构完成改造，建议在可访问 Maven 仓库的环境执行完整构建与压测。
