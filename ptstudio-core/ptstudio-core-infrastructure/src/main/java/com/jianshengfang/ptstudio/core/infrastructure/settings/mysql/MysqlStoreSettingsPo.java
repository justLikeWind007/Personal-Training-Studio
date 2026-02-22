package com.jianshengfang.ptstudio.core.infrastructure.settings.mysql;

import java.time.OffsetDateTime;

public class MysqlStoreSettingsPo {

    private Long id;
    private String tenantKey;
    private String storeKey;
    private String storeName;
    private String businessHoursJson;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getStoreKey() { return storeKey; }
    public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getBusinessHoursJson() { return businessHoursJson; }
    public void setBusinessHoursJson(String businessHoursJson) { this.businessHoursJson = businessHoursJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
