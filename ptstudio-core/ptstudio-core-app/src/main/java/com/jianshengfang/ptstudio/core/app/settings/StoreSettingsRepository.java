package com.jianshengfang.ptstudio.core.app.settings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface StoreSettingsRepository {

    Optional<StoreSettings> get(String tenantId, String storeId);

    StoreSettings save(String tenantId,
                       String storeId,
                       String storeName,
                       String businessHoursJson,
                       OffsetDateTime updatedAt);

    List<StoreSettings> listByTenant(String tenantId);

    Optional<StoreSettings> updateStatus(String tenantId, String storeId, String status, OffsetDateTime updatedAt);
}
