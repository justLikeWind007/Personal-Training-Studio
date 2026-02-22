package com.jianshengfang.ptstudio.core.infrastructure.crm.mysql;

import java.time.OffsetDateTime;

public class MysqlLeadPo {

    private Long id;
    private Long tenantId;
    private Long storeId;
    private String leadNo;
    private String source;
    private String status;
    private String leadName;
    private String mobileHash;
    private Long ownerUserId;
    private OffsetDateTime lastFollowAt;
    private OffsetDateTime nextFollowAt;
    private Long convertedMemberId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getLeadNo() { return leadNo; }
    public void setLeadNo(String leadNo) { this.leadNo = leadNo; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }
    public String getMobileHash() { return mobileHash; }
    public void setMobileHash(String mobileHash) { this.mobileHash = mobileHash; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public OffsetDateTime getLastFollowAt() { return lastFollowAt; }
    public void setLastFollowAt(OffsetDateTime lastFollowAt) { this.lastFollowAt = lastFollowAt; }
    public OffsetDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(OffsetDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public Long getConvertedMemberId() { return convertedMemberId; }
    public void setConvertedMemberId(Long convertedMemberId) { this.convertedMemberId = convertedMemberId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
