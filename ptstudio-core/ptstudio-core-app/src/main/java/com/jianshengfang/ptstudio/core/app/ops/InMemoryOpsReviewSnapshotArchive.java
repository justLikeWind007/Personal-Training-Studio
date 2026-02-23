package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!es")
public class InMemoryOpsReviewSnapshotArchive implements OpsReviewSnapshotArchive {

    private final Map<String, OpsReviewDashboardService.ArchivedReviewSnapshot> latestByKey = new ConcurrentHashMap<>();

    @Override
    public void saveLatest(String tenantId,
                           String storeId,
                           OpsReviewDashboardService.ReviewSnapshot snapshot) {
        OpsReviewDashboardService.ArchivedReviewSnapshot archived = new OpsReviewDashboardService.ArchivedReviewSnapshot(
                tenantId,
                storeId,
                snapshot.dateFrom(),
                snapshot.dateTo(),
                snapshot.totalTasks(),
                snapshot.doneTasks(),
                snapshot.overdueTasks(),
                snapshot.touchCount(),
                snapshot.convertedCount(),
                snapshot.completionRate(),
                snapshot.overdueRate(),
                snapshot.conversionRate(),
                snapshot.avgHandleHours(),
                OffsetDateTime.now()
        );
        latestByKey.put(key(tenantId, storeId), archived);
    }

    @Override
    public Optional<OpsReviewDashboardService.ArchivedReviewSnapshot> latest(String tenantId, String storeId) {
        return Optional.ofNullable(latestByKey.get(key(tenantId, storeId)));
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
    }
}
