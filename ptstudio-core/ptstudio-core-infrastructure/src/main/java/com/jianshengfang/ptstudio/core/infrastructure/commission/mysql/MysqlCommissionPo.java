package com.jianshengfang.ptstudio.core.infrastructure.commission.mysql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class MysqlCommissionPo {

    public static class RulePo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String ruleCode;
        private String ruleName;
        private String calcMode;
        private String ruleJson;
        private Integer version;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getRuleCode() { return ruleCode; }
        public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getCalcMode() { return calcMode; }
        public void setCalcMode(String calcMode) { this.calcMode = calcMode; }
        public String getRuleJson() { return ruleJson; }
        public void setRuleJson(String ruleJson) { this.ruleJson = ruleJson; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public LocalDate getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class StatementPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String statementNo;
        private String statementMonth;
        private Long coachId;
        private Long ruleId;
        private BigDecimal grossAmount;
        private BigDecimal commissionAmount;
        private String status;
        private OffsetDateTime lockedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getStatementNo() { return statementNo; }
        public void setStatementNo(String statementNo) { this.statementNo = statementNo; }
        public String getStatementMonth() { return statementMonth; }
        public void setStatementMonth(String statementMonth) { this.statementMonth = statementMonth; }
        public Long getCoachId() { return coachId; }
        public void setCoachId(Long coachId) { this.coachId = coachId; }
        public Long getRuleId() { return ruleId; }
        public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
        public BigDecimal getGrossAmount() { return grossAmount; }
        public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
        public BigDecimal getCommissionAmount() { return commissionAmount; }
        public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getLockedAt() { return lockedAt; }
        public void setLockedAt(OffsetDateTime lockedAt) { this.lockedAt = lockedAt; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
