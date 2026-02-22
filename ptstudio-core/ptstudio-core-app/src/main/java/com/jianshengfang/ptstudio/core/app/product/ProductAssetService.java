package com.jianshengfang.ptstudio.core.app.product;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductAssetService {

    private final ProductAssetRepository repository;

    public ProductAssetService(ProductAssetRepository repository) {
        this.repository = repository;
    }

    public List<ProductAssetModels.PackageData> listPackages(String tenantId, String storeId) {
        return repository.listPackages(tenantId, storeId);
    }

    public ProductAssetModels.PackageData createPackage(String tenantId,
                                                        String storeId,
                                                        String packageName,
                                                        Integer totalSessions,
                                                        Integer validDays,
                                                        BigDecimal price,
                                                        String saleStatus) {
        validate(totalSessions, validDays, price);
        return repository.createPackage(tenantId, storeId, packageName, totalSessions, validDays, price, saleStatus);
    }

    public ProductAssetModels.PackageData updatePackage(Long id,
                                                        String tenantId,
                                                        String storeId,
                                                        String packageName,
                                                        Integer totalSessions,
                                                        Integer validDays,
                                                        BigDecimal price,
                                                        String saleStatus) {
        validate(totalSessions, validDays, price);
        return repository.updatePackage(id, tenantId, storeId, packageName, totalSessions, validDays, price, saleStatus);
    }

    public List<ProductAssetModels.MemberPackageData> listMemberPackages(String tenantId, String storeId, Long memberId) {
        return repository.listMemberPackages(tenantId, storeId, memberId);
    }

    public List<ProductAssetModels.PackageLedgerData> listMemberPackageLedgers(String tenantId, String storeId, Long memberId) {
        return repository.listMemberPackageLedgers(tenantId, storeId, memberId);
    }

    private void validate(Integer totalSessions, Integer validDays, BigDecimal price) {
        if (totalSessions == null || totalSessions <= 0) {
            throw new IllegalArgumentException("总课时必须大于0");
        }
        if (validDays == null || validDays <= 0) {
            throw new IllegalArgumentException("有效期必须大于0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于0");
        }
    }
}
