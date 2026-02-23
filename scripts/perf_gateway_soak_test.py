#!/usr/bin/env python3
import argparse
import json
import subprocess
import time
from pathlib import Path


def run_once(base_url: str, tenant_id: str, store_id: str, total: int, concurrency: int, timeout: int, output: Path):
    cmd = [
        "python3",
        "scripts/perf_gateway_concurrency.py",
        "--base-url",
        base_url,
        "--tenant-id",
        tenant_id,
        "--store-id",
        store_id,
        "--total",
        str(total),
        "--concurrency",
        str(concurrency),
        "--timeout",
        str(timeout),
        "--output",
        str(output),
    ]
    subprocess.run(cmd, check=True)
    with output.open("r", encoding="utf-8") as f:
        return json.load(f)


def percentile(values, p):
    if not values:
        return 0
    arr = sorted(values)
    idx = int(len(arr) * p) - 1
    idx = max(0, min(idx, len(arr) - 1))
    return arr[idx]


def main():
    parser = argparse.ArgumentParser(description="Gateway 长稳压测（循环执行并汇总）")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--tenant-id", default="tenant-demo")
    parser.add_argument("--store-id", default="store-001")
    parser.add_argument("--total", type=int, default=1200)
    parser.add_argument("--concurrency", type=int, default=80)
    parser.add_argument("--timeout", type=int, default=8)
    parser.add_argument("--duration-min", type=int, default=30, help="总压测时长（分钟）")
    parser.add_argument("--interval-sec", type=int, default=5, help="每轮压测间隔秒数")
    parser.add_argument("--out-dir", default="/tmp/ptstudio-soak")
    parser.add_argument("--summary", default="docs/长稳压测报告_v1.md")
    parser.add_argument("--operator", default="unknown")
    parser.add_argument("--env", default="local-cluster")
    args = parser.parse_args()

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    deadline = time.time() + args.duration_min * 60
    rounds = []
    round_no = 0

    while time.time() < deadline:
        round_no += 1
        out_json = out_dir / f"soak_round_{round_no}.json"
        print(f"[round {round_no}] start")
        data = run_once(
            args.base_url,
            args.tenant_id,
            args.store_id,
            args.total,
            args.concurrency,
            args.timeout,
            out_json,
        )
        rounds.append(data)
        if time.time() + args.interval_sec < deadline:
            time.sleep(args.interval_sec)

    success_rates = []
    rps_values = []
    p95_values = []
    failed_values = []
    limited_values = []
    consumed_values = []
    dead_values = []

    for r in rounds:
        total = int(r.get("total", 0))
        success = int(r.get("success", 0))
        failed = int(r.get("failed", 0))
        limited = int(r.get("limited", 0))
        consumed = int(r.get("consume", {}).get("consumed", 0))
        dead = int(r.get("consume", {}).get("deadCount", 0))
        success_rates.append((success * 100.0 / total) if total > 0 else 0.0)
        rps_values.append(float(r.get("throughputRps", 0)))
        p95_values.append(float(r.get("latencyMs", {}).get("p95", 0)))
        failed_values.append(failed)
        limited_values.append(limited)
        consumed_values.append(consumed)
        dead_values.append(dead)

    def avg(vals):
        return round(sum(vals) / len(vals), 2) if vals else 0

    report_lines = [
        "# 长稳压测报告 v1",
        "",
        f"- 执行环境：{args.env}",
        f"- 执行人：{args.operator}",
        f"- 目标时长（分钟）：{args.duration_min}",
        f"- 实际轮次：{len(rounds)}",
        f"- 每轮参数：total={args.total}, concurrency={args.concurrency}, timeout={args.timeout}s",
        f"- 原始结果目录：`{out_dir}`",
        "",
        "## 1. 汇总指标",
        f"- 平均 success rate：{avg(success_rates)}%",
        f"- 最低 success rate：{round(min(success_rates), 2) if success_rates else 0}%",
        f"- 平均 RPS：{avg(rps_values)}",
        f"- P95 延迟中位值(ms)：{percentile(p95_values, 0.5)}",
        f"- P95 延迟 P95(ms)：{percentile(p95_values, 0.95)}",
        f"- 失败请求峰值：{max(failed_values) if failed_values else 0}",
        f"- 限流请求峰值：{max(limited_values) if limited_values else 0}",
        f"- 消费成功最小值：{min(consumed_values) if consumed_values else 0}",
        f"- deadCount 峰值：{max(dead_values) if dead_values else 0}",
        "",
        "## 2. 稳定性结论",
    ]

    if failed_values and max(failed_values) == 0:
        report_lines.append("- 未出现功能性失败请求，链路稳定。")
    else:
        report_lines.append("- 存在失败请求，需结合网关/服务日志排查高峰期错误。")
    if dead_values and max(dead_values) == 0:
        report_lines.append("- 长稳期间未观察到 dead-letter 增长。")
    else:
        report_lines.append("- 观察到 dead-letter 增长，需排查消费异常和重试策略。")

    report_lines.extend(
        [
            "",
            "## 3. 后续建议",
            "- 将该报告纳入每次版本发布前性能基线归档。",
            "- 若 P95 漂移明显，补充 GC、线程池、连接池与中间件堆积分析。",
            "- 在预发布环境增加故障注入联动压测，验证限流和降级策略。",
            "",
        ]
    )

    summary = Path(args.summary)
    summary.parent.mkdir(parents=True, exist_ok=True)
    summary.write_text("\n".join(report_lines), encoding="utf-8")
    print(f"summary generated: {summary}")


if __name__ == "__main__":
    main()
