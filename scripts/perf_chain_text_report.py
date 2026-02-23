#!/usr/bin/env python3
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path


def load_json_if_exists(path: str):
    if not path:
        return None
    p = Path(path)
    if not p.exists():
        return None
    with p.open("r", encoding="utf-8") as f:
        return json.load(f)


def ratio(numerator: int, denominator: int) -> float:
    if denominator <= 0:
        return 0.0
    return round(numerator * 100.0 / denominator, 2)


def metric(data: dict, key: str, default=0):
    if not data:
        return default
    return data.get(key, default)


def latency(data: dict, key: str, default=0):
    if not data:
        return default
    return data.get("latencyMs", {}).get(key, default)


def consume(data: dict, key: str, default=0):
    if not data:
        return default
    return data.get("consume", {}).get(key, default)


def summary_line(base: dict, limited: dict) -> str:
    if not base and not limited:
        return "未提供 baseline/limited 结果文件，当前文档为模板报告。"
    if limited and int(metric(limited, "limited", 0)) > 0 and int(metric(limited, "failed", 0)) == 0:
        return "限流规则生效且失败请求可控，链路具备在高并发下的保护能力。"
    if limited and int(metric(limited, "limited", 0)) > 0:
        return "限流规则已生效，但存在失败请求，需要补充链路排障。"
    return "暂未观察到明显限流结果，需复核 Sentinel 规则与资源映射。"


def render_report(base: dict, limited: dict, env_name: str, chain_name: str, operator: str) -> str:
    now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S %z")
    base_total = int(metric(base, "total", 0))
    limited_total = int(metric(limited, "total", 0))
    base_success = int(metric(base, "success", 0))
    limited_success = int(metric(limited, "success", 0))
    limited_limited = int(metric(limited, "limited", 0))
    base_fail = int(metric(base, "failed", 0))
    limited_fail = int(metric(limited, "failed", 0))

    return "\n".join(
        [
            "# 链路高并发压测报告 v1",
            "",
            f"- 生成时间：{now}",
            f"- 执行环境：{env_name}",
            f"- 执行人：{operator}",
            f"- 测试链路：`{chain_name}`",
            "",
            "## 1. 测试目标",
            "- 验证 Gateway 入口在高并发场景下的吞吐、延迟与错误率。",
            "- 验证 Sentinel 规则生效后的限流行为是否可预期。",
            "- 验证队列消费闭环（入队/消费/死信）是否可观测。",
            "",
            "## 2. 压测输入",
            "| 场景 | total | concurrency | timeout(s) |",
            "|---|---:|---:|---:|",
            f"| baseline | {base_total} | {int(metric(base, 'concurrency', 0))} | {int(metric(base, 'timeout', 0)) if base else 0} |",
            f"| limited | {limited_total} | {int(metric(limited, 'concurrency', 0))} | {int(metric(limited, 'timeout', 0)) if limited else 0} |",
            "",
            "## 3. 结果总览",
            "| 指标 | baseline | limited |",
            "|---|---:|---:|",
            f"| success | {base_success} | {limited_success} |",
            f"| limited(429) | 0 | {limited_limited} |",
            f"| failed | {base_fail} | {limited_fail} |",
            f"| success rate | {ratio(base_success, base_total)}% | {ratio(limited_success, limited_total)}% |",
            f"| limited rate | 0.00% | {ratio(limited_limited, limited_total)}% |",
            f"| throughput RPS | {metric(base, 'throughputRps', 0)} | {metric(limited, 'throughputRps', 0)} |",
            f"| latency avg(ms) | {latency(base, 'avg', 0)} | {latency(limited, 'avg', 0)} |",
            f"| latency p95(ms) | {latency(base, 'p95', 0)} | {latency(limited, 'p95', 0)} |",
            f"| latency max(ms) | {latency(base, 'max', 0)} | {latency(limited, 'max', 0)} |",
            "",
            "## 4. 消费闭环结果",
            "| 指标 | baseline | limited |",
            "|---|---:|---:|",
            f"| consumeStatus | {consume(base, 'consumeStatus', 'N/A')} | {consume(limited, 'consumeStatus', 'N/A')} |",
            f"| consumed | {consume(base, 'consumed', 0)} | {consume(limited, 'consumed', 0)} |",
            f"| deadCount | {consume(base, 'deadCount', 0)} | {consume(limited, 'deadCount', 0)} |",
            f"| loops | {consume(base, 'loops', 0)} | {consume(limited, 'loops', 0)} |",
            "",
            "## 5. 结论",
            f"- {summary_line(base, limited)}",
            "- 建议将本报告与 `release_precheck`、`security_baseline_check` 一并归档。",
            "",
            "## 6. 风险与改进项",
            "- 若 limited 场景出现高失败率，需核查网关路由、服务实例健康与中间件连接池上限。",
            "- 若队列存在 dead-letter 增长，需对消费逻辑做幂等补偿与异常分类处理。",
            "- 下一阶段建议补充 k6/JMeter 长稳压测（30min+）与混沌注入联动验证。",
            "",
        ]
    ) + "\n"


def parse_args():
    parser = argparse.ArgumentParser(description="生成链路高并发压测文字报告")
    parser.add_argument("--baseline", default="", help="baseline json 文件路径")
    parser.add_argument("--limited", default="", help="limited json 文件路径")
    parser.add_argument("--output", default="docs/链路高并发压测报告_v1.md", help="输出 markdown 文件路径")
    parser.add_argument("--env", default="local-cluster", help="执行环境标识")
    parser.add_argument("--chain", default="Gateway -> Ops Service -> Async Queue", help="链路名称")
    parser.add_argument("--operator", default="unknown", help="执行人")
    return parser.parse_args()


def main():
    args = parse_args()
    base = load_json_if_exists(args.baseline)
    limited = load_json_if_exists(args.limited)
    content = render_report(base, limited, args.env, args.chain, args.operator)
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(content, encoding="utf-8")
    print(f"report generated: {out}")


if __name__ == "__main__":
    main()
