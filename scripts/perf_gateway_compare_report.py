#!/usr/bin/env python3
import argparse
import json
from pathlib import Path


def load_json(path: str):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def ratio(n, d):
    if d <= 0:
        return 0.0
    return round(n * 100.0 / d, 2)


def main():
    parser = argparse.ArgumentParser(description="网关压测前后对比报告")
    parser.add_argument("--baseline", required=True, help="未限流场景结果 json")
    parser.add_argument("--limited", required=True, help="限流场景结果 json")
    parser.add_argument("--output", default="docs/perf_gateway_compare_report.md")
    args = parser.parse_args()

    base = load_json(args.baseline)
    limited = load_json(args.limited)

    base_total = int(base.get("total", 0))
    base_success = int(base.get("success", 0))
    base_fail = int(base.get("failed", 0))

    limited_total = int(limited.get("total", 0))
    limited_success = int(limited.get("success", 0))
    limited_limited = int(limited.get("limited", 0))
    limited_fail = int(limited.get("failed", 0))

    lines = [
        "# Gateway 并发压测对比报告",
        "",
        "## 输入文件",
        f"- baseline: `{args.baseline}`",
        f"- limited: `{args.limited}`",
        "",
        "## 核心指标对比",
        "| 指标 | baseline | limited |",
        "|---|---:|---:|",
        f"| total | {base_total} | {limited_total} |",
        f"| success | {base_success} | {limited_success} |",
        f"| limited(429) | 0 | {limited_limited} |",
        f"| failed | {base_fail} | {limited_fail} |",
        f"| success rate | {ratio(base_success, base_total)}% | {ratio(limited_success, limited_total)}% |",
        f"| limited rate | 0.00% | {ratio(limited_limited, limited_total)}% |",
        f"| throughput RPS | {base.get('throughputRps', 0)} | {limited.get('throughputRps', 0)} |",
        f"| latency p95(ms) | {base.get('latencyMs', {}).get('p95', 0)} | {limited.get('latencyMs', {}).get('p95', 0)} |",
        "",
        "## 结论",
    ]

    if limited_limited > 0 and limited_fail == 0:
        lines.append("- Sentinel 限流生效，系统返回可预期 429，未出现额外异常失败。")
    elif limited_limited > 0:
        lines.append("- Sentinel 限流已生效，但存在额外失败请求，需要排查链路稳定性。")
    else:
        lines.append("- 未观察到限流返回，需检查 Nacos 规则下发、Sentinel 配置与网关路由资源名。")

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"report generated: {args.output}")


if __name__ == "__main__":
    main()
