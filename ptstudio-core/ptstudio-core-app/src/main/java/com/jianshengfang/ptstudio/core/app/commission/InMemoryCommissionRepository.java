package com.jianshengfang.ptstudio.core.app.commission;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!mysql")
public class InMemoryCommissionRepository implements CommissionRepository {

    private final InMemoryCommissionStore store;

    public InMemoryCommissionRepository(InMemoryCommissionStore store) {
        this.store = store;
    }

    @Override
    public List<InMemoryCommissionStore.CommissionRuleData> listRules(String tenantId, String storeId) {
        return store.ruleById().values().stream()
                .filter(rule -> rule.tenantId().equals(tenantId) && rule.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryCommissionStore.CommissionRuleData::id))
                .toList();
    }

    @Override
    public int nextRuleVersion(String tenantId, String storeId) {
        return store.ruleById().values().stream()
                .filter(rule -> rule.tenantId().equals(tenantId) && rule.storeId().equals(storeId))
                .map(InMemoryCommissionStore.CommissionRuleData::version)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    @Override
    public InMemoryCommissionStore.CommissionRuleData createRule(String tenantId, String storeId,
                                                                 String name, String calcMode,
                                                                 BigDecimal ratio, int version,
                                                                 OffsetDateTime effectiveFrom,
                                                                 OffsetDateTime effectiveTo,
                                                                 OffsetDateTime createdAt) {
        long id = store.nextRuleId();
        InMemoryCommissionStore.CommissionRuleData rule = new InMemoryCommissionStore.CommissionRuleData(
                id, tenantId, storeId, name, calcMode, ratio, "ACTIVE", version,
                effectiveFrom, effectiveTo, createdAt, createdAt
        );
        store.ruleById().put(id, rule);
        return rule;
    }

    @Override
    public Optional<InMemoryCommissionStore.CommissionRuleData> getRule(Long ruleId, String tenantId, String storeId) {
        InMemoryCommissionStore.CommissionRuleData rule = store.ruleById().get(ruleId);
        if (rule == null) {
            return Optional.empty();
        }
        if (!rule.tenantId().equals(tenantId) || !rule.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(rule);
    }

    @Override
    public Optional<InMemoryCommissionStore.CommissionStatementData> findStatement(String tenantId, String storeId,
                                                                                    YearMonth statementMonth, Long coachId) {
        return store.statementById().values().stream()
                .filter(statement -> statement.tenantId().equals(tenantId)
                        && statement.storeId().equals(storeId)
                        && statement.statementMonth().equals(statementMonth)
                        && statement.coachId().equals(coachId))
                .findFirst();
    }

    @Override
    public InMemoryCommissionStore.CommissionStatementData createStatement(String tenantId, String storeId,
                                                                           YearMonth statementMonth, Long coachId,
                                                                           Long ruleId, BigDecimal grossAmount,
                                                                           BigDecimal commissionAmount,
                                                                           OffsetDateTime createdAt) {
        long id = store.nextStatementId();
        InMemoryCommissionStore.CommissionStatementData statement = new InMemoryCommissionStore.CommissionStatementData(
                id, tenantId, storeId, statementMonth, coachId, ruleId, grossAmount, commissionAmount,
                "GENERATED", null, createdAt, createdAt
        );
        store.statementById().put(id, statement);
        return statement;
    }

    @Override
    public Optional<InMemoryCommissionStore.CommissionStatementData> getStatement(Long id, String tenantId, String storeId) {
        InMemoryCommissionStore.CommissionStatementData statement = store.statementById().get(id);
        if (statement == null) {
            return Optional.empty();
        }
        if (!statement.tenantId().equals(tenantId) || !statement.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(statement);
    }

    @Override
    public InMemoryCommissionStore.CommissionStatementData lockStatement(Long id, String tenantId,
                                                                         String storeId, OffsetDateTime lockedAt) {
        InMemoryCommissionStore.CommissionStatementData statement = getStatement(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("提成单不存在"));
        InMemoryCommissionStore.CommissionStatementData locked = new InMemoryCommissionStore.CommissionStatementData(
                statement.id(), statement.tenantId(), statement.storeId(), statement.statementMonth(),
                statement.coachId(), statement.ruleId(), statement.grossAmount(), statement.commissionAmount(),
                "LOCKED", lockedAt, statement.createdAt(), lockedAt
        );
        store.statementById().put(id, locked);
        return locked;
    }

    @Override
    public List<InMemoryCommissionStore.CommissionStatementData> listStatements(String tenantId, String storeId,
                                                                                YearMonth statementMonth,
                                                                                String status) {
        return store.statementById().values().stream()
                .filter(statement -> statement.tenantId().equals(tenantId) && statement.storeId().equals(storeId))
                .filter(statement -> statementMonth == null || statement.statementMonth().equals(statementMonth))
                .filter(statement -> status == null || statement.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(InMemoryCommissionStore.CommissionStatementData::id))
                .toList();
    }
}
