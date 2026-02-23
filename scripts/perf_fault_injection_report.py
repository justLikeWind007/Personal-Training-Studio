#!/usr/bin/env python3
import argparse
import json
from pathlib import Path


def load_json(path: str):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def pct(n: int, d: int) -> float:
    if d <= 0:
        return 0.0
    return round((n * 100.0) / d, 2)


def main():
    parser = argparse.ArgumentParser(description="故障注入压测对比报告生成")
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--limited", required=True)
    parser.add_argument("--fault", required=True, help="故障注入场景结果 json")
    parser.add_argument("--fault-name", default="unknown-fault", help="故障名称，例如 nacos-node-down")
    parser.add_argument("--output", default="docs/故障注入压测报告_v1.md")
    parser.add_argument("--operator", default="unknown")
    parser.add_argument("--env", default="local-cluster")
    args = parser.parse_args()

    baseline = load_json(args.baseline)
    limited = load_json(args.limited)
    fault = load_json(args.fault)

    rows = [
        ("baseline", baseline),
        ("limited", limited),
        (f"fault({args.fault_name})", fault),
    ]

    lines = [
        "# 故障注入压测报告 v1",
        "",
        f"- 执行环境：{args.env}",
        f"- 执行人：{args.operator}",
        f"- 故障场景：`{args.fault_name}`",
        "",
        "## 1. 输入文件",
        f"- baseline: `{args.baseline}`",
        f"- limited: `{args.limited}`",
        f"- fault: `{args.fault}`",
        "",
        "## 2. 核心指标对比",
        "| 场景 | total | success | limited | failed | success rate | throughput RPS | p95(ms) | deadCount |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]

    for name, data in rows:
        total = int(data.get("total", 0))
        success = int(data.get("success", 0))
        limited_num = int(data.get("limited", 0))
        failed = int(data.get("failed", 0))
        rps = data.get("throughputRps", 0)
        p95 = data.get("latencyMs", {}).get("p95", 0)
        dead = data.get("consume", {}).get("deadCount", 0)
        lines.append(
            f"| {name} | {total} | {success} | {limited_num} | {failed} | {pct(success, total)}% | {rps} | {p95} | {dead} |"
        )

    lines.extend(["", "## 3. 结论"])
    fault_failed = int(fault.get("failed", 0))
    fault_dead = int(fault.get("consume", {}).get("deadCount", 0))
    if fault_failed == 0 and fault_dead == 0:
        lines.append("- 故障注入期间业务链路仍可维持稳定，无额外失败与死信增长。")
    elif fault_failed > 0 and fault_dead == 0:
        lines.append("- 故障期间出现失败请求，但消费链路未恶化，建议重点优化入口重试与超时配置。")
    else:
        lines.append("- 故障期间出现失败与死信增长，需优化降级与幂等补偿策略。")

    lines.extend(
        [
            "",
            "## 4. 建议动作",
            "- 将该报告纳入故障演练归档，并和发布版本绑定。",
            "- 补充对应故障场景告警阈值与自动化恢复策略。",
            "- 下一轮演练增加更高并发与更长持续时间验证。",
            "",
        ]
    )

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"report generated: {out}")


if __name__ == "__main__":
    main()
