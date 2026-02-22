package com.jianshengfang.ptstudio.core.app.commission;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryCommissionStore {

    private final AtomicLong ruleIdGenerator = new AtomicLong(1);
    private final AtomicLong statementIdGenerator = new AtomicLong(1);

    private final Map<Long, CommissionRuleData> ruleById = new ConcurrentHashMap<>();
    private final Map<Long, CommissionStatementData> statementById = new ConcurrentHashMap<>();

    public long nextRuleId() {
        return ruleIdGenerator.getAndIncrement();
    }

    public long nextStatementId() {
        return statementIdGenerator.getAndIncrement();
    }

    public Map<Long, CommissionRuleData> ruleById() {
        return ruleById;
    }

    public Map<Long, CommissionStatementData> statementById() {
        return statementById;
    }

    public record CommissionRuleData(Long id,
                                     String tenantId,
                                     String storeId,
                                     String name,
                                     String calcMode,
                                     BigDecimal ratio,
                                     String status,
                                     Integer version,
                                     OffsetDateTime effectiveFrom,
                                     OffsetDateTime effectiveTo,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime updatedAt) {
    }

    public record CommissionStatementData(Long id,
                                          String tenantId,
                                          String storeId,
                                          YearMonth statementMonth,
                                          Long coachId,
                                          Long ruleId,
                                          BigDecimal grossAmount,
                                          BigDecimal commissionAmount,
                                          String status,
                                          OffsetDateTime lockedAt,
                                          OffsetDateTime createdAt,
                                          OffsetDateTime updatedAt) {
    }
}
