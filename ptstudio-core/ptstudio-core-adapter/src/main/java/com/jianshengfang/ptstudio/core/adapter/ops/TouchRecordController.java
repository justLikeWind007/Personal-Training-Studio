package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.TouchRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops/touch-records")
@Validated
public class TouchRecordController {

    private final TouchRecordService touchRecordService;

    public TouchRecordController(TouchRecordService touchRecordService) {
        this.touchRecordService = touchRecordService;
    }

    @PostMapping
    @AuditAction(module = "OPS_TOUCH", action = "CREATE")
    public TouchRecordService.TouchRecord create(@Valid @RequestBody CreateTouchRecordRequest request) {
        TenantStoreContext context = requireContext();
        return touchRecordService.create(
                context.tenantId(),
                context.storeId(),
                request.memberId(),
                request.taskNo(),
                request.channel(),
                request.contentSummary(),
                request.result(),
                request.operatorUserId()
        );
    }

    @GetMapping
    public List<TouchRecordService.TouchRecord> list(
            @RequestParam(name = "memberId", required = false) Long memberId,
            @RequestParam(name = "taskNo", required = false) String taskNo) {
        TenantStoreContext context = requireContext();
        return touchRecordService.list(context.tenantId(), context.storeId(), memberId, taskNo);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(name = "memberId", required = false) Long memberId,
            @RequestParam(name = "taskNo", required = false) String taskNo) {
        TenantStoreContext context = requireContext();
        String csv = touchRecordService.exportCsv(context.tenantId(), context.storeId(), memberId, taskNo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"touch_records.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateTouchRecordRequest(@NotNull Long memberId,
                                           String taskNo,
                                           @NotBlank String channel,
                                           @NotBlank String contentSummary,
                                           @NotBlank String result,
                                           @NotNull Long operatorUserId) {
    }
}
