package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.OpsAsyncQueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops/async-queue")
@Validated
public class OpsAsyncQueueController {

    private final OpsAsyncQueueService opsAsyncQueueService;

    public OpsAsyncQueueController(OpsAsyncQueueService opsAsyncQueueService) {
        this.opsAsyncQueueService = opsAsyncQueueService;
    }

    @PostMapping("/enqueue")
    @AuditAction(module = "OPS_ASYNC", action = "ENQUEUE")
    public OpsAsyncQueueService.QueuedEvent enqueue(@Valid @RequestBody EnqueueRequest request) {
        TenantStoreContext context = requireContext();
        return opsAsyncQueueService.enqueue(
                context.tenantId(),
                context.storeId(),
                request.taskNo(),
                request.payload(),
                request.maxRetry(),
                request.operatorUserId()
        );
    }

    @PostMapping("/consume")
    @AuditAction(module = "OPS_ASYNC", action = "CONSUME")
    public OpsAsyncQueueService.ConsumeResult consume(@Valid @RequestBody ConsumeRequest request) {
        TenantStoreContext context = requireContext();
        return opsAsyncQueueService.consume(context.tenantId(), context.storeId(), request.batchSize(), request.operatorUserId());
    }

    @GetMapping("/health")
    public OpsAsyncQueueService.HealthSnapshot health() {
        TenantStoreContext context = requireContext();
        return opsAsyncQueueService.health(context.tenantId(), context.storeId());
    }

    @GetMapping("/dead-letters")
    public List<OpsAsyncQueueService.DeadLetterEvent> deadLetters() {
        TenantStoreContext context = requireContext();
        return opsAsyncQueueService.deadLetters(context.tenantId(), context.storeId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record EnqueueRequest(@NotBlank String taskNo,
                                 @NotBlank String payload,
                                 @NotNull @Min(1) Integer maxRetry,
                                 @NotNull Long operatorUserId) {
    }

    public record ConsumeRequest(@NotNull @Min(1) Integer batchSize,
                                 @NotNull Long operatorUserId) {
    }
}
