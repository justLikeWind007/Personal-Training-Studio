package com.jianshengfang.ptstudio.core.infrastructure.commission.mysql;

import com.jianshengfang.ptstudio.core.app.commission.CommissionRepository;
import com.jianshengfang.ptstudio.core.app.commission.InMemoryCommissionStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlCommissionRepository implements CommissionRepository {

    private final MysqlCommissionMapper mapper;

    public MysqlCommissionRepository(MysqlCommissionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<InMemoryCommissionStore.CommissionRuleData> listRules(String tenantId, String storeId) {
        return mapper.listRules(toLong(tenantId), toLong(storeId)).stream().map(this::toRule).toList();
    }

    @Override
    public int nextRuleVersion(String tenantId, String storeId) {
        return mapper.maxRuleVersion(toLong(tenantId), toLong(storeId)) + 1;
    }

    @Override
    public InMemoryCommissionStore.CommissionRuleData createRule(String tenantId, String storeId,
                                                                 String name, String calcMode,
                                                                 BigDecimal ratio, int version,
                                                                 OffsetDateTime effectiveFrom,
                                                                 OffsetDateTime effectiveTo,
                                                                 OffsetDateTime createdAt) {
        MysqlCommissionPo.RulePo po = new MysqlCommissionPo.RulePo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setRuleCode("RULE");
        po.setRuleName(name);
        po.setCalcMode(calcMode);
        po.setRuleJson("{\"ratio\":" + ratio.toPlainString() + "}");
        po.setVersion(version);
        po.setEffectiveFrom(effectiveFrom.toLocalDate());
        po.setEffectiveTo(effectiveTo == null ? null : effectiveTo.toLocalDate());
        po.setStatus("ACTIVE");
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(createdAt);
        mapper.insertRule(po);
        return toRule(mapper.getRule(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public Optional<InMemoryCommissionStore.CommissionRuleData> getRule(Long ruleId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getRule(ruleId, toLong(tenantId), toLong(storeId))).map(this::toRule);
    }

    @Override
    public Optional<InMemoryCommissionStore.CommissionStatementData> findStatement(String tenantId,
                                                                                    String storeId,
                                                                                    YearMonth statementMonth,
                                                                                    Long coachId) {
        return Optional.ofNullable(mapper.findStatement(
                toLong(tenantId), toLong(storeId), statementMonth.toString(), coachId)).map(this::toStatement);
    }

    @Override
    public InMemoryCommissionStore.CommissionStatementData createStatement(String tenantId, String storeId,
                                                                           YearMonth statementMonth, Long coachId,
                                                                           Long ruleId, BigDecimal grossAmount,
                                                                           BigDecimal commissionAmount,
                                                                           OffsetDateTime createdAt) {
        MysqlCommissionPo.StatementPo po = new MysqlCommissionPo.StatementPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setStatementNo("ST" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        po.setStatementMonth(statementMonth.toString());
        po.setCoachId(coachId);
        po.setRuleId(ruleId);
        po.setGrossAmount(grossAmount);
        po.setCommissionAmount(commissionAmount);
        po.setStatus("GENERATED");
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(createdAt);
        mapper.insertStatement(po);
        return toStatement(mapper.getStatement(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public Optional<InMemoryCommissionStore.CommissionStatementData> getStatement(Long id, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getStatement(id, toLong(tenantId), toLong(storeId))).map(this::toStatement);
    }

    @Override
    public InMemoryCommissionStore.CommissionStatementData lockStatement(Long id, String tenantId,
                                                                         String storeId, OffsetDateTime lockedAt) {
        long t = toLong(tenantId);
        long s = toLong(storeId);
        mapper.lockStatement(id, t, s, lockedAt);
        return toStatement(mapper.getStatement(id, t, s));
    }

    @Override
    public List<InMemoryCommissionStore.CommissionStatementData> listStatements(String tenantId, String storeId,
                                                                                YearMonth statementMonth,
                                                                                String status) {
        return mapper.listStatements(
                toLong(tenantId), toLong(storeId),
                statementMonth == null ? null : statementMonth.toString(),
                status
        ).stream().map(this::toStatement).toList();
    }

    private InMemoryCommissionStore.CommissionRuleData toRule(MysqlCommissionPo.RulePo po) {
        BigDecimal ratio = BigDecimal.ZERO;
        if (po.getRuleJson() != null && po.getRuleJson().contains("ratio")) {
            String raw = po.getRuleJson().replaceAll("[^0-9.]", "");
            if (!raw.isBlank()) {
                ratio = new BigDecimal(raw);
            }
        }
        return new InMemoryCommissionStore.CommissionRuleData(
                po.getId(), String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()),
                po.getRuleName(), po.getCalcMode(), ratio, po.getStatus(), po.getVersion(),
                po.getEffectiveFrom().atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                po.getEffectiveTo() == null ? null : po.getEffectiveTo().atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private InMemoryCommissionStore.CommissionStatementData toStatement(MysqlCommissionPo.StatementPo po) {
        return new InMemoryCommissionStore.CommissionStatementData(
                po.getId(), String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()),
                YearMonth.parse(po.getStatementMonth()), po.getCoachId(), po.getRuleId(),
                po.getGrossAmount(), po.getCommissionAmount(), po.getStatus(),
                po.getLockedAt(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private long toLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1L;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 1L;
        }
        return Long.parseLong(digits);
    }
}
