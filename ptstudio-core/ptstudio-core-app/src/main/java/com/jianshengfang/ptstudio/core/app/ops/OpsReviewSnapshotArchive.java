package com.jianshengfang.ptstudio.core.app.ops;

import java.util.Optional;

public interface OpsReviewSnapshotArchive {

    void saveLatest(String tenantId,
                    String storeId,
                    OpsReviewDashboardService.ReviewSnapshot snapshot);

    Optional<OpsReviewDashboardService.ArchivedReviewSnapshot> latest(String tenantId, String storeId);
}
