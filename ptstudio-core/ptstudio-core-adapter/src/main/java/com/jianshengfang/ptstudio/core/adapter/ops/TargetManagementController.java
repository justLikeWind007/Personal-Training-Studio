package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.TargetManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/ops/targets")
public class TargetManagementController {

    private final TargetManagementService targetManagementService;

    public TargetManagementController(TargetManagementService targetManagementService) {
        this.targetManagementService = targetManagementService;
    }

    @PutMapping
    @AuditAction(module = "OPS_TARGET", action = "UPSERT")
    public TargetManagementService.MonthlyTarget upsert(@Valid @RequestBody UpsertTargetRequest request) {
        TenantStoreContext context = requireContext();
        YearMonth month = YearMonth.parse(request.month());
        return targetManagementService.upsert(
                context.tenantId(),
                context.storeId(),
                month,
                request.revenueTarget(),
                request.consumptionTarget(),
                request.newMemberTarget()
        );
    }

    @GetMapping("/achievement")
    public TargetManagementService.MonthlyAchievement achievement(@RequestParam("month") @NotBlank String month) {
        TenantStoreContext context = requireContext();
        return targetManagementService.achievement(context.tenantId(), context.storeId(), YearMonth.parse(month));
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record UpsertTargetRequest(@NotBlank String month,
                                      @NotNull @DecimalMin("0") BigDecimal revenueTarget,
                                      @NotNull Integer consumptionTarget,
                                      @NotNull Integer newMemberTarget) {
    }
}
