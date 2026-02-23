# Local Middleware Cluster Environment (v5)

## 1. 启动集群拓扑

```bash
docker compose -f docker-compose.cluster.yml up -d
```

核心入口：
- Nacos 集群入口: `http://127.0.0.1:8848/nacos`
- Redis Master: `127.0.0.1:16379`
- Redis Sentinel: `127.0.0.1:26379,26380,26381`
- RocketMQ NameServer: `127.0.0.1:19876,29876`
- ES 集群入口: `http://127.0.0.1:19200`
- Kibana: `http://127.0.0.1:15601`

## 2. 集群冒烟

```bash
./scripts/middleware_cluster_smoke.sh
```

## 2.1 Redis Sentinel 切换演练

```bash
./scripts/failover_drill_redis_sentinel.sh
```

## 2.2 Elasticsearch 单节点故障演练

```bash
./scripts/failover_drill_es_cluster.sh
```

## 2.3 Nacos 节点故障演练

```bash
./scripts/failover_drill_nacos_cluster.sh
```

## 3. 适用场景

- 验证 Spring Cloud Alibaba 服务注册在 Nacos 集群下的可用性
- 验证 Redis Sentinel 故障切换前置配置
- 验证 RocketMQ 多 NameServer 基础可用性
- 验证 Elasticsearch 三节点集群健康

## 4. 注意事项

- `docker-compose.cluster.yml` 作为开发/测试环境集群拓扑参考，不直接等价生产编排。
- Nacos 使用外置 MySQL 时需先初始化 `nacos_config` 数据库结构（建议在项目脚本中固化 SQL 初始化步骤）。
- 生产建议迁移到 Kubernetes 或云厂商托管中间件，并补齐持久化与备份策略。
