package com.jianshengfang.ptstudio.core.infrastructure.crm.mysql;

import java.time.OffsetDateTime;

public class MysqlLeadFollowPo {

    private Long id;
    private Long tenantId;
    private Long storeId;
    private Long leadId;
    private String followType;
    private String content;
    private OffsetDateTime nextFollowAt;
    private Long followerUserId;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    public String getFollowType() { return followType; }
    public void setFollowType(String followType) { this.followType = followType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public OffsetDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(OffsetDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public Long getFollowerUserId() { return followerUserId; }
    public void setFollowerUserId(Long followerUserId) { this.followerUserId = followerUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
