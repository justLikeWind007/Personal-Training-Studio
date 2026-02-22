package com.jianshengfang.ptstudio.core.adapter.settings;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettings;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stores")
@Validated
public class StoreOrgAdminController {

    private final StoreSettingsService storeSettingsService;

    public StoreOrgAdminController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public List<StoreSettings> listStores() {
        TenantStoreContext context = requireContext();
        return storeSettingsService.listByTenant(context.tenantId());
    }

    @PostMapping
    @AuditAction(module = "STORE_ORG", action = "CREATE")
    public StoreSettings createStore(@Valid @RequestBody CreateStoreRequest request) {
        TenantStoreContext context = requireContext();
        return storeSettingsService.createStore(
                context.tenantId(),
                request.storeId(),
                request.storeName(),
                request.businessHoursJson()
        );
    }

    @PostMapping("/{storeId}/status")
    @AuditAction(module = "STORE_ORG", action = "UPDATE_STATUS")
    public StoreSettings updateStatus(@PathVariable String storeId,
                                      @Valid @RequestBody UpdateStoreStatusRequest request) {
        TenantStoreContext context = requireContext();
        return storeSettingsService.updateStoreStatus(context.tenantId(), storeId, request.status());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateStoreRequest(@NotBlank String storeId,
                                     @NotBlank String storeName,
                                     @NotBlank String businessHoursJson) {
    }

    public record UpdateStoreStatusRequest(@NotBlank String status) {
    }
}
