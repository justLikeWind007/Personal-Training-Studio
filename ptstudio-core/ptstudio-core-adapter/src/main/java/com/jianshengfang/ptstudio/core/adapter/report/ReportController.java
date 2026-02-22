package com.jianshengfang.ptstudio.core.adapter.report;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.report.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public ReportService.OverviewReport overview() {
        TenantStoreContext context = requireContext();
        return reportService.overview(context.tenantId(), context.storeId());
    }

    @GetMapping("/attendance")
    public ReportService.AttendanceReport attendance() {
        TenantStoreContext context = requireContext();
        return reportService.attendance(context.tenantId(), context.storeId());
    }

    @GetMapping("/finance")
    public ReportService.FinanceReport finance() {
        TenantStoreContext context = requireContext();
        return reportService.finance(context.tenantId(), context.storeId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }
}
