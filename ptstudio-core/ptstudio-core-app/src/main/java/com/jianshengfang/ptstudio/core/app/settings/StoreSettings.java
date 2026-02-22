package com.jianshengfang.ptstudio.core.app.settings;

import java.time.OffsetDateTime;

public record StoreSettings(String tenantId,
                            String storeId,
                            String storeName,
                            String businessHoursJson,
                            String status,
                            OffsetDateTime updatedAt) {
}
