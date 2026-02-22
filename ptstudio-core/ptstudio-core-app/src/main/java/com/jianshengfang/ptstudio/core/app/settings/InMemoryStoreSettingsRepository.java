package com.jianshengfang.ptstudio.core.app.settings;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
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
        String key = key(tenantId, storeId);
        String status = Optional.ofNullable(data.get(key)).map(StoreSettings::status).orElse("ACTIVE");
        StoreSettings settings = new StoreSettings(tenantId, storeId, storeName, businessHoursJson, status, updatedAt);
        data.put(key(tenantId, storeId), settings);
        return settings;
    }

    @Override
    public List<StoreSettings> listByTenant(String tenantId) {
        return data.values().stream()
                .filter(settings -> settings.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(StoreSettings::storeId))
                .toList();
    }

    @Override
    public Optional<StoreSettings> updateStatus(String tenantId, String storeId, String status, OffsetDateTime updatedAt) {
        String key = key(tenantId, storeId);
        StoreSettings existing = data.get(key);
        if (existing == null) {
            return Optional.empty();
        }
        StoreSettings updated = new StoreSettings(
                existing.tenantId(),
                existing.storeId(),
                existing.storeName(),
                existing.businessHoursJson(),
                status,
                updatedAt
        );
        data.put(key, updated);
        return Optional.of(updated);
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "::" + storeId;
    }
}
