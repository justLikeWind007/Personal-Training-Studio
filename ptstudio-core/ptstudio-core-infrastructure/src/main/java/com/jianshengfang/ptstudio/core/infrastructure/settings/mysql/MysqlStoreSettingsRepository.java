package com.jianshengfang.ptstudio.core.infrastructure.settings.mysql;

import com.jianshengfang.ptstudio.core.app.settings.StoreSettings;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlStoreSettingsRepository implements StoreSettingsRepository {

    private final MysqlStoreSettingsMapper mapper;

    public MysqlStoreSettingsRepository(MysqlStoreSettingsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<StoreSettings> get(String tenantId, String storeId) {
        return Optional.ofNullable(mapper.get(tenantId, storeId)).map(this::toDomain);
    }

    @Override
    public StoreSettings save(String tenantId, String storeId, String storeName,
                              String businessHoursJson, OffsetDateTime updatedAt) {
        MysqlStoreSettingsPo existing = mapper.get(tenantId, storeId);
        if (existing == null) {
            MysqlStoreSettingsPo po = new MysqlStoreSettingsPo();
            po.setTenantKey(tenantId);
            po.setStoreKey(storeId);
            po.setStoreName(storeName);
            po.setBusinessHoursJson(businessHoursJson);
            po.setStatus("ACTIVE");
            po.setCreatedAt(updatedAt);
            po.setUpdatedAt(updatedAt);
            mapper.insert(po);
            return toDomain(mapper.get(tenantId, storeId));
        }

        mapper.update(tenantId, storeId, storeName, businessHoursJson, updatedAt);
        return toDomain(mapper.get(tenantId, storeId));
    }

    @Override
    public List<StoreSettings> listByTenant(String tenantId) {
        return mapper.listByTenant(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<StoreSettings> updateStatus(String tenantId, String storeId, String status, OffsetDateTime updatedAt) {
        mapper.updateStatus(tenantId, storeId, status, updatedAt);
        return Optional.ofNullable(mapper.get(tenantId, storeId)).map(this::toDomain);
    }

    private StoreSettings toDomain(MysqlStoreSettingsPo po) {
        return new StoreSettings(
                po.getTenantKey(),
                po.getStoreKey(),
                po.getStoreName(),
                po.getBusinessHoursJson(),
                po.getStatus(),
                po.getUpdatedAt()
        );
    }
}
