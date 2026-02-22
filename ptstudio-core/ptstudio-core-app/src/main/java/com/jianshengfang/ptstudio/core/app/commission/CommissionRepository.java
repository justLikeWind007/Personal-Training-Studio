package com.jianshengfang.ptstudio.core.app.commission;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CommissionRepository {

    List<InMemoryCommissionStore.CommissionRuleData> listRules(String tenantId, String storeId);

    int nextRuleVersion(String tenantId, String storeId);

    InMemoryCommissionStore.CommissionRuleData createRule(String tenantId,
                                                          String storeId,
                                                          String name,
                                                          String calcMode,
                                                          BigDecimal ratio,
                                                          int version,
                                                          OffsetDateTime effectiveFrom,
                                                          OffsetDateTime effectiveTo,
                                                          OffsetDateTime createdAt);

    Optional<InMemoryCommissionStore.CommissionRuleData> getRule(Long ruleId, String tenantId, String storeId);

    Optional<InMemoryCommissionStore.CommissionStatementData> findStatement(String tenantId,
                                                                             String storeId,
                                                                             YearMonth statementMonth,
                                                                             Long coachId);

    InMemoryCommissionStore.CommissionStatementData createStatement(String tenantId,
                                                                    String storeId,
                                                                    YearMonth statementMonth,
                                                                    Long coachId,
                                                                    Long ruleId,
                                                                    BigDecimal grossAmount,
                                                                    BigDecimal commissionAmount,
                                                                    OffsetDateTime createdAt);

    Optional<InMemoryCommissionStore.CommissionStatementData> getStatement(Long id, String tenantId, String storeId);

    InMemoryCommissionStore.CommissionStatementData lockStatement(Long id,
                                                                  String tenantId,
                                                                  String storeId,
                                                                  OffsetDateTime lockedAt);

    List<InMemoryCommissionStore.CommissionStatementData> listStatements(String tenantId,
                                                                         String storeId,
                                                                         YearMonth statementMonth,
                                                                         String status);
}
