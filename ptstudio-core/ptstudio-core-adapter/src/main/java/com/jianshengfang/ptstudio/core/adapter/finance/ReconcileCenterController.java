package com.jianshengfang.ptstudio.core.adapter.finance;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.finance.ReconcileCenterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reconcile-center")
@Validated
public class ReconcileCenterController {

    private final ReconcileCenterService reconcileCenterService;

    public ReconcileCenterController(ReconcileCenterService reconcileCenterService) {
        this.reconcileCenterService = reconcileCenterService;
    }

    @GetMapping("/overview")
    public ReconcileCenterService.ReconcileOverview overview(
            @RequestParam(name = "bizDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate bizDate) {
        TenantStoreContext context = requireContext();
        return reconcileCenterService.overview(context.tenantId(), context.storeId(), bizDate);
    }

    @GetMapping("/issues")
    public List<ReconcileCenterService.ReconcileIssue> issues(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type) {
        TenantStoreContext context = requireContext();
        return reconcileCenterService.listIssues(context.tenantId(), context.storeId(), status, type);
    }

    @PostMapping("/issues/{issueNo}/retry")
    @AuditAction(module = "RECONCILE", action = "ISSUE_RETRY")
    public ReconcileCenterService.ReconcileIssue retry(@PathVariable String issueNo,
                                                       @Valid @RequestBody IssueActionRequest request) {
        TenantStoreContext context = requireContext();
        return reconcileCenterService.retryIssue(context.tenantId(), context.storeId(), issueNo, request.operatorUserId());
    }

    @PostMapping("/issues/{issueNo}/close")
    @AuditAction(module = "RECONCILE", action = "ISSUE_CLOSE")
    public ReconcileCenterService.ReconcileIssue close(@PathVariable String issueNo,
                                                       @Valid @RequestBody IssueActionRequest request) {
        TenantStoreContext context = requireContext();
        return reconcileCenterService.closeIssue(context.tenantId(), context.storeId(), issueNo, request.operatorUserId());
    }

    @GetMapping("/issues/export")
    public ResponseEntity<String> export(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type) {
        TenantStoreContext context = requireContext();
        String csv = reconcileCenterService.exportIssuesCsv(context.tenantId(), context.storeId(), status, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reconcile_issues.csv\"")
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

    public record IssueActionRequest(@NotNull Long operatorUserId) {
    }
}
