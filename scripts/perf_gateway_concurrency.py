#!/usr/bin/env python3
import argparse
import json
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://127.0.0.1:8080"
TENANT_ID = "tenant-demo"
STORE_ID = "store-001"
TOTAL = 2000
CONCURRENCY = 100
TIMEOUT = 8


def http_post(path: str, body: dict):
    req = urllib.request.Request(
        url=BASE_URL + path,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "X-Tenant-Id": TENANT_ID,
            "X-Store-Id": STORE_ID,
        },
        method="POST",
    )
    start = time.time()
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            payload = resp.read().decode("utf-8")
            latency_ms = (time.time() - start) * 1000
            return resp.status, payload, latency_ms
    except urllib.error.HTTPError as ex:
        latency_ms = (time.time() - start) * 1000
        return ex.code, ex.read().decode("utf-8", errors="ignore"), latency_ms
    except Exception as ex:
        latency_ms = (time.time() - start) * 1000
        return 0, str(ex), latency_ms


def http_get(path: str):
    req = urllib.request.Request(
        url=BASE_URL + path,
        headers={
            "X-Tenant-Id": TENANT_ID,
            "X-Store-Id": STORE_ID,
        },
        method="GET",
    )
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return resp.read().decode("utf-8")


def enqueue_one(i: int):
    return http_post(
        "/api/ops/async-queue/enqueue",
        {
            "taskNo": f"LOAD-{i}",
            "payload": "LOAD_TEST",
            "maxRetry": 3,
            "operatorUserId": 9001,
        },
    )


def consume_until_empty():
    total_success = 0
    loops = 0
    while loops < 200:
        loops += 1
        status, payload, _ = http_post(
            "/api/ops/async-queue/consume",
            {"batchSize": 200, "operatorUserId": 9002},
        )
        if status != 200:
            return {"consumeStatus": status, "consumeError": payload, "loops": loops}

        data = json.loads(payload)
        total_success += int(data.get("successCount", 0))
        health = json.loads(http_get("/api/ops/async-queue/health"))
        if int(health.get("queueSize", 0)) <= 0:
            return {
                "consumeStatus": 200,
                "consumed": total_success,
                "deadCount": int(health.get("deadCount", 0)),
                "loops": loops,
            }
    return {"consumeStatus": 500, "consumeError": "queue not drained", "loops": loops}


def p95(values):
    if not values:
        return 0
    data = sorted(values)
    idx = int(len(data) * 0.95) - 1
    idx = max(0, min(idx, len(data) - 1))
    return data[idx]


def parse_args():
    parser = argparse.ArgumentParser(description="gateway->ops async queue 并发压测")
    parser.add_argument("--base-url", default=BASE_URL)
    parser.add_argument("--tenant-id", default=TENANT_ID)
    parser.add_argument("--store-id", default=STORE_ID)
    parser.add_argument("--total", type=int, default=TOTAL)
    parser.add_argument("--concurrency", type=int, default=CONCURRENCY)
    parser.add_argument("--timeout", type=int, default=TIMEOUT)
    parser.add_argument("--output", default="", help="将结果写入 json 文件")
    parser.add_argument("--expect-limited-min", type=int, default=0, help="期望最小限流数量")
    return parser.parse_args()


def main():
    global BASE_URL, TENANT_ID, STORE_ID, TOTAL, CONCURRENCY, TIMEOUT
    args = parse_args()
    BASE_URL = args.base_url
    TENANT_ID = args.tenant_id
    STORE_ID = args.store_id
    TOTAL = args.total
    CONCURRENCY = args.concurrency
    TIMEOUT = args.timeout

    print(f"start load test: total={TOTAL}, concurrency={CONCURRENCY}")
    latencies = []
    success = 0
    failed = 0
    limited = 0

    t0 = time.time()
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
        futures = [pool.submit(enqueue_one, i) for i in range(TOTAL)]
        for fut in as_completed(futures):
            status, _, latency = fut.result()
            latencies.append(latency)
            if status == 200:
                success += 1
            elif status == 429:
                limited += 1
            else:
                failed += 1
    elapsed = time.time() - t0

    consume_result = consume_until_empty()

    result = {
        "total": TOTAL,
        "concurrency": CONCURRENCY,
        "success": success,
        "failed": failed,
        "limited": limited,
        "elapsedSeconds": round(elapsed, 3),
        "throughputRps": round((success + limited) / elapsed, 2) if elapsed > 0 else 0,
        "latencyMs": {
            "avg": round(sum(latencies) / len(latencies), 2) if latencies else 0,
            "p95": round(p95(latencies), 2),
            "max": round(max(latencies), 2) if latencies else 0,
        },
        "consume": consume_result,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(f"result saved: {args.output}")

    if args.expect_limited_min > 0 and limited < args.expect_limited_min:
        raise SystemExit(
            f"limited count {limited} below expected minimum {args.expect_limited_min}"
        )


if __name__ == "__main__":
    main()
