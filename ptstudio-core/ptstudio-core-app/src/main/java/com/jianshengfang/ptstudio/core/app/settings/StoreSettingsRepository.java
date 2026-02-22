package com.jianshengfang.ptstudio.core.app.settings;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface StoreSettingsRepository {

    Optional<StoreSettings> get(String tenantId, String storeId);

    StoreSettings save(String tenantId,
                       String storeId,
                       String storeName,
                       String businessHoursJson,
                       OffsetDateTime updatedAt);
}
