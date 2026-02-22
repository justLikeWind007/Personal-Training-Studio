package com.jianshengfang.ptstudio.core.infrastructure.product.mysql;

import com.jianshengfang.ptstudio.core.app.product.ProductAssetModels;
import com.jianshengfang.ptstudio.core.app.product.ProductAssetRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlProductAssetRepository implements ProductAssetRepository {

    private final MysqlProductAssetMapper mapper;

    public MysqlProductAssetRepository(MysqlProductAssetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProductAssetModels.PackageData> listPackages(String tenantId, String storeId) {
        return mapper.listPackages(toLong(tenantId), toLong(storeId)).stream().map(this::toPackage).toList();
    }

    @Override
    public ProductAssetModels.PackageData createPackage(String tenantId, String storeId, String packageName,
                                                        Integer totalSessions, Integer validDays,
                                                        BigDecimal price, String saleStatus) {
        MysqlProductAssetPo.PackagePo po = new MysqlProductAssetPo.PackagePo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setPackageCode("PKG" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        po.setPackageName(packageName);
        po.setTotalSessions(totalSessions);
        po.setValidDays(validDays);
        po.setPrice(price);
        po.setSaleStatus(saleStatus);
        mapper.insertPackage(po);
        return toPackage(mapper.getPackage(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public Optional<ProductAssetModels.PackageData> getPackage(Long id, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getPackage(id, toLong(tenantId), toLong(storeId))).map(this::toPackage);
    }

    @Override
    public ProductAssetModels.PackageData updatePackage(Long id, String tenantId, String storeId,
                                                        String packageName, Integer totalSessions,
                                                        Integer validDays, BigDecimal price,
                                                        String saleStatus) {
        MysqlProductAssetPo.PackagePo po = mapper.getPackage(id, toLong(tenantId), toLong(storeId));
        if (po == null) {
            throw new IllegalArgumentException("套餐不存在");
        }
        po.setPackageName(packageName);
        po.setTotalSessions(totalSessions);
        po.setValidDays(validDays);
        po.setPrice(price);
        po.setSaleStatus(saleStatus);
        mapper.updatePackage(po);
        return toPackage(mapper.getPackage(id, po.getTenantId(), po.getStoreId()));
    }

    @Override
    public List<ProductAssetModels.MemberPackageData> listMemberPackages(String tenantId, String storeId, Long memberId) {
        long t = toLong(tenantId);
        long s = toLong(storeId);
        List<MysqlProductAssetPo.MemberPackagePo> current = mapper.listMemberPackages(t, s, memberId);
        if (!current.isEmpty()) {
            return current.stream().map(this::toMemberPackage).toList();
        }

        MysqlProductAssetPo.PackagePo firstPackage = mapper.listPackages(t, s).stream().findFirst().orElse(null);
        if (firstPackage == null) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now();
        MysqlProductAssetPo.MemberPackagePo account = new MysqlProductAssetPo.MemberPackagePo();
        account.setTenantId(t);
        account.setStoreId(s);
        account.setAccountNo("MPA" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        account.setMemberId(memberId);
        account.setPackageId(firstPackage.getId());
        account.setTotalSessions(firstPackage.getTotalSessions());
        account.setUsedSessions(0);
        account.setRemainingSessions(firstPackage.getTotalSessions());
        account.setExpireAt(now.plusDays(firstPackage.getValidDays()));
        account.setStatus("ACTIVE");
        mapper.insertMemberPackage(account);

        MysqlProductAssetPo.LedgerPo ledger = new MysqlProductAssetPo.LedgerPo();
        ledger.setTenantId(t);
        ledger.setStoreId(s);
        ledger.setAccountId(account.getId());
        ledger.setActionType("INIT");
        ledger.setSessionsDelta(firstPackage.getTotalSessions());
        ledger.setBeforeSessions(0);
        ledger.setAfterSessions(firstPackage.getTotalSessions());
        ledger.setBizType("SYSTEM");
        ledger.setBizId(account.getId());
        ledger.setOperatorUserId(0L);
        mapper.insertLedger(ledger);

        return mapper.listMemberPackages(t, s, memberId).stream().map(this::toMemberPackage).toList();
    }

    @Override
    public List<ProductAssetModels.PackageLedgerData> listMemberPackageLedgers(String tenantId, String storeId, Long memberId) {
        return mapper.listMemberLedgers(toLong(tenantId), toLong(storeId), memberId).stream()
                .map(this::toLedger)
                .toList();
    }

    private ProductAssetModels.PackageData toPackage(MysqlProductAssetPo.PackagePo po) {
        return new ProductAssetModels.PackageData(
                po.getId(), String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()),
                po.getPackageCode(), po.getPackageName(), po.getTotalSessions(), po.getValidDays(),
                po.getPrice(), po.getSaleStatus(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private ProductAssetModels.MemberPackageData toMemberPackage(MysqlProductAssetPo.MemberPackagePo po) {
        return new ProductAssetModels.MemberPackageData(
                po.getId(), String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()),
                po.getAccountNo(), po.getMemberId(), po.getPackageId(), po.getTotalSessions(),
                po.getUsedSessions(), po.getRemainingSessions(), po.getExpireAt(), po.getStatus(),
                po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private ProductAssetModels.PackageLedgerData toLedger(MysqlProductAssetPo.LedgerPo po) {
        return new ProductAssetModels.PackageLedgerData(
                po.getId(), String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()),
                po.getAccountId(), po.getActionType(), po.getSessionsDelta(), po.getBeforeSessions(),
                po.getAfterSessions(), po.getBizType(), po.getBizId(), po.getOperatorUserId(), po.getOccurredAt()
        );
    }

    private long toLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1L;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 1L;
        }
        return Long.parseLong(digits);
    }
}
