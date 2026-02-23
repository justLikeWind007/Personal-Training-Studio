package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OpsReviewArchiveHealthTracker {

    private final Map<String, ArchiveHealthSnapshot> healthByStore = new ConcurrentHashMap<>();

    public void markSuccess(String tenantId, String storeId) {
        String key = key(tenantId, storeId);
        ArchiveHealthSnapshot current = healthByStore.get(key);
        OffsetDateTime successAt = OffsetDateTime.now();
        OffsetDateTime lastFailureAt = current == null ? null : current.lastFailureAt();
        healthByStore.put(key, new ArchiveHealthSnapshot("UP", successAt, lastFailureAt, ""));
    }

    public void markFailure(String tenantId, String storeId, String error) {
        String key = key(tenantId, storeId);
        ArchiveHealthSnapshot current = healthByStore.get(key);
        OffsetDateTime lastSuccessAt = current == null ? null : current.lastSuccessAt();
        healthByStore.put(key, new ArchiveHealthSnapshot(
                "DEGRADED",
                lastSuccessAt,
                OffsetDateTime.now(),
                error == null ? "UNKNOWN_ERROR" : error
        ));
    }

    public ArchiveHealthSnapshot snapshot(String tenantId, String storeId) {
        return healthByStore.getOrDefault(key(tenantId, storeId),
                new ArchiveHealthSnapshot("UNKNOWN", null, null, ""));
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
    }

    public record ArchiveHealthSnapshot(String status,
                                        OffsetDateTime lastSuccessAt,
                                        OffsetDateTime lastFailureAt,
                                        String lastError) {
    }
}
