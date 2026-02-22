package com.jianshengfang.ptstudio.core.app.product;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!mysql")
public class InMemoryProductAssetRepository implements ProductAssetRepository {

    private final AtomicLong packageIdGen = new AtomicLong(1);
    private final AtomicLong accountIdGen = new AtomicLong(1);
    private final AtomicLong ledgerIdGen = new AtomicLong(1);

    private final Map<Long, ProductAssetModels.PackageData> packageMap = new ConcurrentHashMap<>();
    private final Map<Long, ProductAssetModels.MemberPackageData> accountMap = new ConcurrentHashMap<>();
    private final Map<Long, ProductAssetModels.PackageLedgerData> ledgerMap = new ConcurrentHashMap<>();

    @Override
    public List<ProductAssetModels.PackageData> listPackages(String tenantId, String storeId) {
        return packageMap.values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.storeId().equals(storeId))
                .sorted(Comparator.comparing(ProductAssetModels.PackageData::id))
                .toList();
    }

    @Override
    public ProductAssetModels.PackageData createPackage(String tenantId, String storeId, String packageName,
                                                        Integer totalSessions, Integer validDays,
                                                        BigDecimal price, String saleStatus) {
        long id = packageIdGen.getAndIncrement();
        OffsetDateTime now = OffsetDateTime.now();
        ProductAssetModels.PackageData data = new ProductAssetModels.PackageData(
                id, tenantId, storeId,
                "PKG" + String.format("%06d", id), packageName, totalSessions, validDays,
                price, saleStatus, now, now
        );
        packageMap.put(id, data);
        return data;
    }

    @Override
    public Optional<ProductAssetModels.PackageData> getPackage(Long id, String tenantId, String storeId) {
        ProductAssetModels.PackageData data = packageMap.get(id);
        if (data == null) {
            return Optional.empty();
        }
        if (!data.tenantId().equals(tenantId) || !data.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(data);
    }

    @Override
    public ProductAssetModels.PackageData updatePackage(Long id, String tenantId, String storeId,
                                                        String packageName, Integer totalSessions,
                                                        Integer validDays, BigDecimal price,
                                                        String saleStatus) {
        ProductAssetModels.PackageData old = getPackage(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("套餐不存在"));
        ProductAssetModels.PackageData data = new ProductAssetModels.PackageData(
                old.id(), old.tenantId(), old.storeId(), old.packageCode(),
                packageName, totalSessions, validDays, price, saleStatus,
                old.createdAt(), OffsetDateTime.now()
        );
        packageMap.put(id, data);
        return data;
    }

    @Override
    public List<ProductAssetModels.MemberPackageData> listMemberPackages(String tenantId, String storeId, Long memberId) {
        var data = accountMap.values().stream()
                .filter(a -> a.tenantId().equals(tenantId) && a.storeId().equals(storeId) && a.memberId().equals(memberId))
                .sorted(Comparator.comparing(ProductAssetModels.MemberPackageData::id))
                .toList();
        if (!data.isEmpty()) {
            return data;
        }

        ProductAssetModels.PackageData firstPackage = listPackages(tenantId, storeId).stream().findFirst().orElse(null);
        if (firstPackage == null) {
            return List.of();
        }

        long accountId = accountIdGen.getAndIncrement();
        OffsetDateTime now = OffsetDateTime.now();
        ProductAssetModels.MemberPackageData account = new ProductAssetModels.MemberPackageData(
                accountId, tenantId, storeId,
                "MPA" + String.format("%06d", accountId), memberId, firstPackage.id(),
                firstPackage.totalSessions(), 0, firstPackage.totalSessions(),
                now.plusDays(firstPackage.validDays()), "ACTIVE", now, now
        );
        accountMap.put(accountId, account);

        long ledgerId = ledgerIdGen.getAndIncrement();
        ledgerMap.put(ledgerId, new ProductAssetModels.PackageLedgerData(
                ledgerId, tenantId, storeId, accountId,
                "INIT", firstPackage.totalSessions(), 0, firstPackage.totalSessions(),
                "SYSTEM", accountId, 0L, now
        ));

        return List.of(account);
    }

    @Override
    public List<ProductAssetModels.PackageLedgerData> listMemberPackageLedgers(String tenantId, String storeId, Long memberId) {
        List<Long> accountIds = listMemberPackages(tenantId, storeId, memberId).stream().map(ProductAssetModels.MemberPackageData::id).toList();
        return ledgerMap.values().stream()
                .filter(l -> l.tenantId().equals(tenantId) && l.storeId().equals(storeId))
                .filter(l -> accountIds.contains(l.accountId()))
                .sorted(Comparator.comparing(ProductAssetModels.PackageLedgerData::id))
                .toList();
    }
}
