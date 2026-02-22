package com.jianshengfang.ptstudio.core.app.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductAssetRepository {

    List<ProductAssetModels.PackageData> listPackages(String tenantId, String storeId);

    ProductAssetModels.PackageData createPackage(String tenantId,
                                                 String storeId,
                                                 String packageName,
                                                 Integer totalSessions,
                                                 Integer validDays,
                                                 BigDecimal price,
                                                 String saleStatus);

    Optional<ProductAssetModels.PackageData> getPackage(Long id, String tenantId, String storeId);

    ProductAssetModels.PackageData updatePackage(Long id,
                                                 String tenantId,
                                                 String storeId,
                                                 String packageName,
                                                 Integer totalSessions,
                                                 Integer validDays,
                                                 BigDecimal price,
                                                 String saleStatus);

    List<ProductAssetModels.MemberPackageData> listMemberPackages(String tenantId, String storeId, Long memberId);

    List<ProductAssetModels.PackageLedgerData> listMemberPackageLedgers(String tenantId, String storeId, Long memberId);
}
