#!/usr/bin/env bash
set -euo pipefail

NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"
GROUP="${GROUP:-SENTINEL_GROUP}"
OPS_DATA_ID="${OPS_DATA_ID:-ptstudio-ops-service-flow-rules}"
GW_DATA_ID="${GW_DATA_ID:-ptstudio-cloud-gateway-gw-flow-rules}"
OPS_QPS="${OPS_QPS:-80}"
GW_QPS="${GW_QPS:-120}"

OPS_RULES="[{\"resource\":\"/api/ops/async-queue/enqueue\",\"limitApp\":\"default\",\"grade\":1,\"count\":${OPS_QPS},\"strategy\":0,\"controlBehavior\":0,\"clusterMode\":false}]"
GW_RULES="[{\"resource\":\"/api/ops/async-queue/enqueue\",\"resourceMode\":0,\"grade\":1,\"count\":${GW_QPS},\"intervalSec\":1,\"controlBehavior\":0}]"

echo "publish ops flow rules to nacos: dataId=${OPS_DATA_ID}, qps=${OPS_QPS}"
curl -fsS -X POST "http://${NACOS_SERVER_ADDR}/nacos/v1/cs/configs" \
  --data-urlencode "dataId=${OPS_DATA_ID}" \
  --data-urlencode "group=${GROUP}" \
  --data-urlencode "content=${OPS_RULES}" >/dev/null

echo "publish gateway flow rules to nacos: dataId=${GW_DATA_ID}, qps=${GW_QPS}"
curl -fsS -X POST "http://${NACOS_SERVER_ADDR}/nacos/v1/cs/configs" \
  --data-urlencode "dataId=${GW_DATA_ID}" \
  --data-urlencode "group=${GROUP}" \
  --data-urlencode "content=${GW_RULES}" >/dev/null

echo "nacos sentinel flow rules published"
