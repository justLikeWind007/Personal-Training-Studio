package com.jianshengfang.ptstudio.core.app.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StoreSettingsService {

    private static final String DEFAULT_STORE_NAME = "默认门店";
    private static final String DEFAULT_HOURS = "{\"weekdays\":\"09:00-21:00\",\"weekend\":\"10:00-20:00\"}";

    private final StoreSettingsRepository repository;

    public StoreSettingsService(StoreSettingsRepository repository) {
        this.repository = repository;
    }

    public List<StoreSettings> listByTenant(String tenantId) {
        return repository.listByTenant(tenantId);
    }

    public StoreSettings get(String tenantId, String storeId) {
        return repository.get(tenantId, storeId)
                .orElseGet(() -> repository.save(tenantId, storeId, DEFAULT_STORE_NAME, DEFAULT_HOURS, OffsetDateTime.now()));
    }

    @Transactional
    public StoreSettings update(String tenantId, String storeId, String storeName, String businessHoursJson) {
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("门店名称不能为空");
        }
        if (businessHoursJson == null || businessHoursJson.isBlank()) {
            throw new IllegalArgumentException("营业时间配置不能为空");
        }
        return repository.save(tenantId, storeId, storeName, businessHoursJson, OffsetDateTime.now());
    }

    @Transactional
    public StoreSettings createStore(String tenantId, String storeId, String storeName, String businessHoursJson) {
        if (storeId == null || storeId.isBlank()) {
            throw new IllegalArgumentException("门店编码不能为空");
        }
        if (repository.get(tenantId, storeId).isPresent()) {
            throw new IllegalArgumentException("门店已存在");
        }
        String hours = (businessHoursJson == null || businessHoursJson.isBlank()) ? DEFAULT_HOURS : businessHoursJson;
        String name = (storeName == null || storeName.isBlank()) ? DEFAULT_STORE_NAME : storeName;
        return repository.save(tenantId, storeId, name, hours, OffsetDateTime.now());
    }

    @Transactional
    public StoreSettings updateStoreStatus(String tenantId, String storeId, String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException("门店状态仅支持 ACTIVE/INACTIVE");
        }
        return repository.updateStatus(tenantId, storeId, status, OffsetDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("门店不存在"));
    }

    public boolean isStoreWritable(String tenantId, String storeId) {
        Optional<StoreSettings> store = repository.get(tenantId, storeId);
        if (store.isEmpty()) {
            return true;
        }
        return !"INACTIVE".equals(store.get().status());
    }
}
