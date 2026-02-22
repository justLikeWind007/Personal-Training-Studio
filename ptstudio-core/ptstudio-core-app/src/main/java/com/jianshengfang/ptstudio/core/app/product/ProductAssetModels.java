package com.jianshengfang.ptstudio.core.app.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ProductAssetModels {

    public record PackageData(Long id,
                              String tenantId,
                              String storeId,
                              String packageCode,
                              String packageName,
                              Integer totalSessions,
                              Integer validDays,
                              BigDecimal price,
                              String saleStatus,
                              OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
    }

    public record MemberPackageData(Long id,
                                    String tenantId,
                                    String storeId,
                                    String accountNo,
                                    Long memberId,
                                    Long packageId,
                                    Integer totalSessions,
                                    Integer usedSessions,
                                    Integer remainingSessions,
                                    OffsetDateTime expireAt,
                                    String status,
                                    OffsetDateTime createdAt,
                                    OffsetDateTime updatedAt) {
    }

    public record PackageLedgerData(Long id,
                                    String tenantId,
                                    String storeId,
                                    Long accountId,
                                    String actionType,
                                    Integer sessionsDelta,
                                    Integer beforeSessions,
                                    Integer afterSessions,
                                    String bizType,
                                    Long bizId,
                                    Long operatorUserId,
                                    OffsetDateTime occurredAt) {
    }
}
