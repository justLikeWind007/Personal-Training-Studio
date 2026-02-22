package com.jianshengfang.ptstudio.core.infrastructure.product.mysql;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MysqlProductAssetPo {

    public static class PackagePo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String packageCode;
        private String packageName;
        private Integer totalSessions;
        private Integer validDays;
        private BigDecimal price;
        private String saleStatus;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getPackageCode() { return packageCode; }
        public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public Integer getTotalSessions() { return totalSessions; }
        public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
        public Integer getValidDays() { return validDays; }
        public void setValidDays(Integer validDays) { this.validDays = validDays; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getSaleStatus() { return saleStatus; }
        public void setSaleStatus(String saleStatus) { this.saleStatus = saleStatus; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class MemberPackagePo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String accountNo;
        private Long memberId;
        private Long packageId;
        private Integer totalSessions;
        private Integer usedSessions;
        private Integer remainingSessions;
        private OffsetDateTime expireAt;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public Long getPackageId() { return packageId; }
        public void setPackageId(Long packageId) { this.packageId = packageId; }
        public Integer getTotalSessions() { return totalSessions; }
        public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
        public Integer getUsedSessions() { return usedSessions; }
        public void setUsedSessions(Integer usedSessions) { this.usedSessions = usedSessions; }
        public Integer getRemainingSessions() { return remainingSessions; }
        public void setRemainingSessions(Integer remainingSessions) { this.remainingSessions = remainingSessions; }
        public OffsetDateTime getExpireAt() { return expireAt; }
        public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class LedgerPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private Long accountId;
        private String actionType;
        private Integer sessionsDelta;
        private Integer beforeSessions;
        private Integer afterSessions;
        private String bizType;
        private Long bizId;
        private Long operatorUserId;
        private OffsetDateTime occurredAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public Integer getSessionsDelta() { return sessionsDelta; }
        public void setSessionsDelta(Integer sessionsDelta) { this.sessionsDelta = sessionsDelta; }
        public Integer getBeforeSessions() { return beforeSessions; }
        public void setBeforeSessions(Integer beforeSessions) { this.beforeSessions = beforeSessions; }
        public Integer getAfterSessions() { return afterSessions; }
        public void setAfterSessions(Integer afterSessions) { this.afterSessions = afterSessions; }
        public String getBizType() { return bizType; }
        public void setBizType(String bizType) { this.bizType = bizType; }
        public Long getBizId() { return bizId; }
        public void setBizId(Long bizId) { this.bizId = bizId; }
        public Long getOperatorUserId() { return operatorUserId; }
        public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
        public OffsetDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    }
}
