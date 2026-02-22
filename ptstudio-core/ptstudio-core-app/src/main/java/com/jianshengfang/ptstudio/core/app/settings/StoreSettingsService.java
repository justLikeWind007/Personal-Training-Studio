package com.jianshengfang.ptstudio.core.app.settings;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class StoreSettingsService {

    private static final String DEFAULT_STORE_NAME = "默认门店";
    private static final String DEFAULT_HOURS = "{\"weekdays\":\"09:00-21:00\",\"weekend\":\"10:00-20:00\"}";

    private final StoreSettingsRepository repository;

    public StoreSettingsService(StoreSettingsRepository repository) {
        this.repository = repository;
    }

    public StoreSettings get(String tenantId, String storeId) {
        return repository.get(tenantId, storeId)
                .orElseGet(() -> repository.save(tenantId, storeId, DEFAULT_STORE_NAME, DEFAULT_HOURS, OffsetDateTime.now()));
    }

    public StoreSettings update(String tenantId, String storeId, String storeName, String businessHoursJson) {
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("门店名称不能为空");
        }
        if (businessHoursJson == null || businessHoursJson.isBlank()) {
            throw new IllegalArgumentException("营业时间配置不能为空");
        }
        return repository.save(tenantId, storeId, storeName, businessHoursJson, OffsetDateTime.now());
    }
}
