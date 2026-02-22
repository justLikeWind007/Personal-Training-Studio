package com.jianshengfang.ptstudio.core.app.settings;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryStoreSettingsRepository implements StoreSettingsRepository {

    private final Map<String, StoreSettings> data = new ConcurrentHashMap<>();

    @Override
    public Optional<StoreSettings> get(String tenantId, String storeId) {
        String key = key(tenantId, storeId);
        return Optional.ofNullable(data.get(key));
    }

    @Override
    public StoreSettings save(String tenantId, String storeId, String storeName,
                              String businessHoursJson, OffsetDateTime updatedAt) {
        StoreSettings settings = new StoreSettings(tenantId, storeId, storeName, businessHoursJson, updatedAt);
        data.put(key(tenantId, storeId), settings);
        return settings;
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "::" + storeId;
    }
}
