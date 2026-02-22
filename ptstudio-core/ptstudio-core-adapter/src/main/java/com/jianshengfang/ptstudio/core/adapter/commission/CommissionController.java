package com.jianshengfang.ptstudio.core.adapter.commission;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.commission.CommissionService;
import com.jianshengfang.ptstudio.core.app.commission.InMemoryCommissionStore;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/commission")
@Validated
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping("/rules")
    public List<InMemoryCommissionStore.CommissionRuleData> listRules() {
        TenantStoreContext context = requireContext();
        return commissionService.listRules(context.tenantId(), context.storeId());
    }

    @PostMapping("/rules")
    @AuditAction(module = "COMMISSION_RULE", action = "CREATE")
    public InMemoryCommissionStore.CommissionRuleData createRule(@Valid @RequestBody CreateRuleRequest request) {
        TenantStoreContext context = requireContext();
        return commissionService.createRule(new CommissionService.CreateRuleCommand(
                context.tenantId(),
                context.storeId(),
                request.name(),
                request.calcMode(),
                request.ratio(),
                request.effectiveFrom(),
                request.effectiveTo()
        ));
    }

    @PostMapping("/statements/generate")
    @AuditAction(module = "COMMISSION_STATEMENT", action = "GENERATE")
    public List<InMemoryCommissionStore.CommissionStatementData> generateStatements(
            @Valid @RequestBody GenerateStatementRequest request) {
        TenantStoreContext context = requireContext();
        return commissionService.generateStatements(new CommissionService.GenerateStatementCommand(
                context.tenantId(),
                context.storeId(),
                request.statementMonth(),
                request.ruleId()
        ));
    }

    @PostMapping("/statements/{id}/lock")
    @AuditAction(module = "COMMISSION_STATEMENT", action = "LOCK")
    public InMemoryCommissionStore.CommissionStatementData lock(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return commissionService.lockStatement(id, context.tenantId(), context.storeId());
    }

    @GetMapping("/statements")
    public List<InMemoryCommissionStore.CommissionStatementData> listStatements(
            @RequestParam(name = "statementMonth", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth statementMonth,
            @RequestParam(name = "status", required = false) String status) {
        TenantStoreContext context = requireContext();
        return commissionService.listStatements(context.tenantId(), context.storeId(), statementMonth, status);
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateRuleRequest(@NotBlank String name,
                                    @NotBlank String calcMode,
                                    @NotNull @DecimalMin("0.01") @DecimalMax("1.00") BigDecimal ratio,
                                    @NotNull OffsetDateTime effectiveFrom,
                                    OffsetDateTime effectiveTo) {
    }

    public record GenerateStatementRequest(@NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth statementMonth,
                                           @NotNull Long ruleId) {
    }
}
