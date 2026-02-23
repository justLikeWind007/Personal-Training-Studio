package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.JourneyNodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/journey-nodes")
public class JourneyNodeController {

    private final JourneyNodeService journeyNodeService;

    public JourneyNodeController(JourneyNodeService journeyNodeService) {
        this.journeyNodeService = journeyNodeService;
    }

    @PostMapping("/scan")
    @AuditAction(module = "OPS_JOURNEY", action = "SCAN")
    public JourneyNodeService.ScanResult scan(@Valid @RequestBody ScanRequest request) {
        TenantStoreContext context = requireContext();
        return journeyNodeService.scan(
                context.tenantId(),
                context.storeId(),
                request.autoGenerateTask(),
                request.operatorUserId()
        );
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record ScanRequest(boolean autoGenerateTask,
                              @NotNull Long operatorUserId) {
    }
}
