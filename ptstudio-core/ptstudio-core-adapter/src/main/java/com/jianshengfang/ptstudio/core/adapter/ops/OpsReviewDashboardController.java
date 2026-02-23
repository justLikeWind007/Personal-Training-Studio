package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.OpsReviewArchiveHealthTracker;
import com.jianshengfang.ptstudio.core.app.ops.OpsReviewDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ops/review-dashboard")
public class OpsReviewDashboardController {

    private final OpsReviewDashboardService opsReviewDashboardService;
    private final OpsReviewArchiveHealthTracker archiveHealthTracker;

    public OpsReviewDashboardController(OpsReviewDashboardService opsReviewDashboardService,
                                        OpsReviewArchiveHealthTracker archiveHealthTracker) {
        this.opsReviewDashboardService = opsReviewDashboardService;
        this.archiveHealthTracker = archiveHealthTracker;
    }

    @GetMapping
    public OpsReviewDashboardService.ReviewSnapshot snapshot(
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        TenantStoreContext context = requireContext();
        return opsReviewDashboardService.snapshot(context.tenantId(), context.storeId(), dateFrom, dateTo);
    }

    @GetMapping("/latest")
    public OpsReviewDashboardService.ArchivedReviewSnapshot latest() {
        TenantStoreContext context = requireContext();
        return opsReviewDashboardService.latest(context.tenantId(), context.storeId());
    }

    @GetMapping("/archive-health")
    public OpsReviewArchiveHealthTracker.ArchiveHealthSnapshot archiveHealth() {
        TenantStoreContext context = requireContext();
        return archiveHealthTracker.snapshot(context.tenantId(), context.storeId());
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        TenantStoreContext context = requireContext();
        String csv = opsReviewDashboardService.exportCsv(context.tenantId(), context.storeId(), dateFrom, dateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ops_review_dashboard.csv\"")
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
}
