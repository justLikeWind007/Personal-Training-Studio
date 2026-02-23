# CI 分层测试执行说明

本项目已将自动化测试按风险与执行时长分为三层，供 CI 按阶段触发：

- `smoke`：基础可用性验证，适合每次提交快速反馈。
- `security`：租户/门店隔离、RBAC、数据权限等安全回归。
- `regression`：业务主链路回归（当前包含稽核中心回归）。

## Maven Profiles

根 `pom.xml` 已提供以下 profile：

- `ci-smoke` -> 运行 `@Tag("smoke")`
- `ci-security` -> 运行 `@Tag("security")`
- `ci-regression` -> 运行 `@Tag("regression")`

示例命令：

```bash
mvn -q -pl ptstudio-start -am -Pci-smoke test
mvn -q -pl ptstudio-start -am -Pci-security test
mvn -q -pl ptstudio-start -am -Pci-regression test
```

## 统一脚本

脚本：`scripts/ci_layered_tests.sh`

```bash
./scripts/ci_layered_tests.sh smoke
./scripts/ci_layered_tests.sh security
./scripts/ci_layered_tests.sh regression
./scripts/ci_layered_tests.sh all
```

建议 CI 流水线阶段：

1. `smoke`：PR 创建/更新即触发。
2. `security`：PR 合并前必须通过。
3. `regression`：主分支定时 + 发版前执行。

## 当前注意事项

- 当前分层框架已可执行，但历史测试用例仍有失败项需要继续修复后再作为强门禁。
- 建议先在 `smoke` 层做必过门禁，再逐步提升到 `security/regression` 强制通过。
