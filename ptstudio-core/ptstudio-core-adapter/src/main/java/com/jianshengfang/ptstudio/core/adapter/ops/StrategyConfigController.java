package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.StrategyConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/ops/strategies")
@Validated
public class StrategyConfigController {

    private final StrategyConfigService strategyConfigService;

    public StrategyConfigController(StrategyConfigService strategyConfigService) {
        this.strategyConfigService = strategyConfigService;
    }

    @PutMapping("/draft")
    @AuditAction(module = "OPS_STRATEGY", action = "SAVE_DRAFT")
    public StrategyConfigService.StrategyDraft saveDraft(@Valid @RequestBody SaveDraftRequest request) {
        TenantStoreContext context = requireContext();
        return strategyConfigService.saveDraft(
                context.tenantId(),
                context.storeId(),
                request.refundRiskRatioThreshold(),
                request.lowAttendanceRateThreshold(),
                request.reversedConsumptionDailyThreshold(),
                request.metricCaliber(),
                request.remark(),
                request.operatorUserId()
        );
    }

    @PostMapping("/publish")
    @AuditAction(module = "OPS_STRATEGY", action = "PUBLISH")
    public StrategyConfigService.StrategyVersion publish(@Valid @RequestBody OperatorRequest request) {
        TenantStoreContext context = requireContext();
        return strategyConfigService.publish(context.tenantId(), context.storeId(), request.operatorUserId());
    }

    @PostMapping("/rollback/{versionNo}")
    @AuditAction(module = "OPS_STRATEGY", action = "ROLLBACK")
    public StrategyConfigService.StrategyVersion rollback(@PathVariable("versionNo") String versionNo,
                                                          @Valid @RequestBody OperatorRequest request) {
        TenantStoreContext context = requireContext();
        return strategyConfigService.rollback(
                context.tenantId(),
                context.storeId(),
                versionNo,
                request.operatorUserId()
        );
    }

    @GetMapping("/current")
    public StrategyConfigService.StrategyVersion current() {
        TenantStoreContext context = requireContext();
        return strategyConfigService.current(context.tenantId(), context.storeId());
    }

    @GetMapping("/history")
    public List<StrategyConfigService.StrategyVersion> history() {
        TenantStoreContext context = requireContext();
        return strategyConfigService.history(context.tenantId(), context.storeId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record SaveDraftRequest(@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal refundRiskRatioThreshold,
                                   @NotNull Integer lowAttendanceRateThreshold,
                                   @NotNull Integer reversedConsumptionDailyThreshold,
                                   @NotBlank String metricCaliber,
                                   String remark,
                                   @NotNull Long operatorUserId) {
    }

    public record OperatorRequest(@NotNull Long operatorUserId) {
    }
}
