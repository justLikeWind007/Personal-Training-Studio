package com.jianshengfang.ptstudio.core.adapter.settings;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettings;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/store")
@Validated
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public StoreSettings getStoreSettings() {
        TenantStoreContext context = requireContext();
        return storeSettingsService.get(context.tenantId(), context.storeId());
    }

    @PutMapping
    public StoreSettings updateStoreSettings(@Valid @RequestBody UpdateStoreSettingsRequest request) {
        TenantStoreContext context = requireContext();
        return storeSettingsService.update(context.tenantId(), context.storeId(), request.storeName(), request.businessHoursJson());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record UpdateStoreSettingsRequest(@NotBlank String storeName,
                                             @NotBlank String businessHoursJson) {
    }
}
