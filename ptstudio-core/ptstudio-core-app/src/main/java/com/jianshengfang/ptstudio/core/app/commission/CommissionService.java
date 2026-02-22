package com.jianshengfang.ptstudio.core.app.commission;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommissionService {

    private final InMemoryCommissionStore store;
    private final InMemoryScheduleStore scheduleStore;

    public CommissionService(InMemoryCommissionStore store, InMemoryScheduleStore scheduleStore) {
        this.store = store;
        this.scheduleStore = scheduleStore;
    }

    public List<InMemoryCommissionStore.CommissionRuleData> listRules(String tenantId, String storeId) {
        return store.ruleById().values().stream()
                .filter(rule -> rule.tenantId().equals(tenantId) && rule.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryCommissionStore.CommissionRuleData::id))
                .toList();
    }

    public InMemoryCommissionStore.CommissionRuleData createRule(CreateRuleCommand command) {
        if (command.ratio().compareTo(BigDecimal.ZERO) <= 0 || command.ratio().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("提成比例必须在(0,1]区间");
        }

        int nextVersion = store.ruleById().values().stream()
                .filter(rule -> rule.tenantId().equals(command.tenantId()) && rule.storeId().equals(command.storeId()))
                .map(InMemoryCommissionStore.CommissionRuleData::version)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        long id = store.nextRuleId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCommissionStore.CommissionRuleData rule = new InMemoryCommissionStore.CommissionRuleData(
                id,
                command.tenantId(),
                command.storeId(),
                command.name(),
                command.calcMode(),
                command.ratio(),
                "ACTIVE",
                nextVersion,
                command.effectiveFrom(),
                command.effectiveTo(),
                now,
                now
        );
        store.ruleById().put(id, rule);
        return rule;
    }

    public List<InMemoryCommissionStore.CommissionStatementData> generateStatements(GenerateStatementCommand command) {
        InMemoryCommissionStore.CommissionRuleData rule = Optional.ofNullable(store.ruleById().get(command.ruleId()))
                .orElseThrow(() -> new IllegalArgumentException("提成规则不存在"));
        if (!rule.tenantId().equals(command.tenantId()) || !rule.storeId().equals(command.storeId())) {
            throw new IllegalArgumentException("提成规则不存在");
        }

        List<Long> coachIds = scheduleStore.coachById().values().stream()
                .filter(c -> c.tenantId().equals(command.tenantId()) && c.storeId().equals(command.storeId()))
                .map(InMemoryScheduleStore.CoachData::id)
                .sorted()
                .collect(Collectors.toList());

        if (coachIds.isEmpty()) {
            throw new IllegalArgumentException("当前门店暂无教练，无法生成提成单");
        }

        return coachIds.stream().map(coachId -> {
            boolean exists = store.statementById().values().stream()
                    .anyMatch(statement -> statement.tenantId().equals(command.tenantId())
                            && statement.storeId().equals(command.storeId())
                            && statement.statementMonth().equals(command.statementMonth())
                            && statement.coachId().equals(coachId));
            if (exists) {
                return store.statementById().values().stream()
                        .filter(statement -> statement.tenantId().equals(command.tenantId())
                                && statement.storeId().equals(command.storeId())
                                && statement.statementMonth().equals(command.statementMonth())
                                && statement.coachId().equals(coachId))
                        .findFirst()
                        .orElseThrow();
            }

            BigDecimal grossAmount = BigDecimal.valueOf(1000 + coachId * 100);
            BigDecimal commissionAmount = grossAmount.multiply(rule.ratio())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            long statementId = store.nextStatementId();
            OffsetDateTime now = OffsetDateTime.now();
            InMemoryCommissionStore.CommissionStatementData statement = new InMemoryCommissionStore.CommissionStatementData(
                    statementId,
                    command.tenantId(),
                    command.storeId(),
                    command.statementMonth(),
                    coachId,
                    rule.id(),
                    grossAmount,
                    commissionAmount,
                    "GENERATED",
                    null,
                    now,
                    now
            );
            store.statementById().put(statementId, statement);
            return statement;
        }).toList();
    }

    public InMemoryCommissionStore.CommissionStatementData lockStatement(Long id, String tenantId, String storeId) {
        InMemoryCommissionStore.CommissionStatementData statement = Optional.ofNullable(store.statementById().get(id))
                .orElseThrow(() -> new IllegalArgumentException("提成单不存在"));
        if (!statement.tenantId().equals(tenantId) || !statement.storeId().equals(storeId)) {
            throw new IllegalArgumentException("提成单不存在");
        }
        if (!statement.status().equals("GENERATED")) {
            throw new IllegalArgumentException("当前状态不可锁账");
        }

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCommissionStore.CommissionStatementData locked = new InMemoryCommissionStore.CommissionStatementData(
                statement.id(),
                statement.tenantId(),
                statement.storeId(),
                statement.statementMonth(),
                statement.coachId(),
                statement.ruleId(),
                statement.grossAmount(),
                statement.commissionAmount(),
                "LOCKED",
                now,
                statement.createdAt(),
                now
        );
        store.statementById().put(locked.id(), locked);
        return locked;
    }

    public List<InMemoryCommissionStore.CommissionStatementData> listStatements(String tenantId,
                                                                                 String storeId,
                                                                                 YearMonth statementMonth,
                                                                                 String status) {
        return store.statementById().values().stream()
                .filter(statement -> statement.tenantId().equals(tenantId) && statement.storeId().equals(storeId))
                .filter(statement -> statementMonth == null || statement.statementMonth().equals(statementMonth))
                .filter(statement -> status == null || statement.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(InMemoryCommissionStore.CommissionStatementData::id))
                .toList();
    }

    public record CreateRuleCommand(String tenantId,
                                    String storeId,
                                    String name,
                                    String calcMode,
                                    BigDecimal ratio,
                                    OffsetDateTime effectiveFrom,
                                    OffsetDateTime effectiveTo) {
    }

    public record GenerateStatementCommand(String tenantId,
                                           String storeId,
                                           YearMonth statementMonth,
                                           Long ruleId) {
    }
}
