package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.OperationTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops/tasks")
@Validated
public class OperationTaskController {

    private final OperationTaskService operationTaskService;

    public OperationTaskController(OperationTaskService operationTaskService) {
        this.operationTaskService = operationTaskService;
    }

    @PutMapping("/rules")
    @AuditAction(module = "OPS_TASK", action = "RULE_SAVE")
    public OperationTaskService.TaskRule saveRule(@Valid @RequestBody SaveRuleRequest request) {
        TenantStoreContext context = requireContext();
        return operationTaskService.saveRule(
                context.tenantId(),
                context.storeId(),
                request.triggerType(),
                request.priority(),
                request.ownerRole(),
                request.titleTemplate(),
                request.generateLimit(),
                request.operatorUserId()
        );
    }

    @PostMapping("/generate")
    @AuditAction(module = "OPS_TASK", action = "GENERATE")
    public OperationTaskService.GenerateResult generate(@Valid @RequestBody OperatorRequest request) {
        TenantStoreContext context = requireContext();
        return operationTaskService.generate(context.tenantId(), context.storeId(), request.operatorUserId());
    }

    @GetMapping
    public List<OperationTaskService.OperationTask> list(
            @RequestParam(name = "status", required = false) String status) {
        TenantStoreContext context = requireContext();
        return operationTaskService.list(context.tenantId(), context.storeId(), status);
    }

    @PostMapping("/{taskNo}/start")
    @AuditAction(module = "OPS_TASK", action = "START")
    public OperationTaskService.OperationTask start(@PathVariable("taskNo") String taskNo,
                                                    @Valid @RequestBody OperatorRequest request) {
        TenantStoreContext context = requireContext();
        return operationTaskService.start(context.tenantId(), context.storeId(), taskNo, request.operatorUserId());
    }

    @PostMapping("/{taskNo}/complete")
    @AuditAction(module = "OPS_TASK", action = "COMPLETE")
    public OperationTaskService.OperationTask complete(@PathVariable("taskNo") String taskNo,
                                                       @Valid @RequestBody OperatorRequest request) {
        TenantStoreContext context = requireContext();
        return operationTaskService.complete(context.tenantId(), context.storeId(), taskNo, request.operatorUserId());
    }

    @PostMapping("/{taskNo}/close")
    @AuditAction(module = "OPS_TASK", action = "CLOSE")
    public OperationTaskService.OperationTask close(@PathVariable("taskNo") String taskNo,
                                                    @Valid @RequestBody OperatorRequest request) {
        TenantStoreContext context = requireContext();
        return operationTaskService.close(context.tenantId(), context.storeId(), taskNo, request.operatorUserId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record SaveRuleRequest(@NotBlank String triggerType,
                                  @NotBlank String priority,
                                  @NotBlank String ownerRole,
                                  @NotBlank String titleTemplate,
                                  @NotNull @Min(1) Integer generateLimit,
                                  @NotNull Long operatorUserId) {
    }

    public record OperatorRequest(@NotNull Long operatorUserId) {
    }
}
