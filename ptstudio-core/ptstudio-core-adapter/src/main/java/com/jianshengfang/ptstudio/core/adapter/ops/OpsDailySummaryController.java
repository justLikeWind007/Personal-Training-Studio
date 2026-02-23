package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.OpsDailySummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ops")
public class OpsDailySummaryController {

    private final OpsDailySummaryService opsDailySummaryService;

    public OpsDailySummaryController(OpsDailySummaryService opsDailySummaryService) {
        this.opsDailySummaryService = opsDailySummaryService;
    }

    @GetMapping("/daily-summary")
    public OpsDailySummaryService.DailySummary dailySummary(
            @RequestParam(name = "bizDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate) {
        TenantStoreContext context = requireContext();
        return opsDailySummaryService.summary(context.tenantId(), context.storeId(), bizDate);
    }

    @GetMapping("/daily-summary/text")
    public DailySummaryTextResponse dailySummaryText(
            @RequestParam(name = "bizDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate) {
        TenantStoreContext context = requireContext();
        OpsDailySummaryService.DailySummary summary = opsDailySummaryService.summary(
                context.tenantId(), context.storeId(), bizDate);
        String text = opsDailySummaryService.text(summary, context.storeId());
        return new DailySummaryTextResponse(summary.bizDate(), text);
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record DailySummaryTextResponse(LocalDate bizDate,
                                           String text) {
    }
}
