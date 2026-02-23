package com.jianshengfang.ptstudio.core.app.report;

import com.jianshengfang.ptstudio.core.app.finance.FinanceService;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettings;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class HqReportService {

    private final ReportService reportService;
    private final FinanceService financeService;
    private final StoreSettingsService storeSettingsService;
    private final AtomicLong metricVersion = new AtomicLong(1L);
    private final Map<String, MetricDefinition> metricDefinitions = new ConcurrentHashMap<>();

    public HqReportService(ReportService reportService,
                           FinanceService financeService,
                           StoreSettingsService storeSettingsService) {
        this.reportService = reportService;
        this.financeService = financeService;
        this.storeSettingsService = storeSettingsService;
        initDefaultMetrics();
    }

    public HqOverview overview(String tenantId, List<String> storeIds) {
        List<StoreSnapshot> snapshots = storeSnapshots(tenantId, storeIds);
        BigDecimal totalRevenue = snapshots.stream().map(StoreSnapshot::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefund = snapshots.stream().map(StoreSnapshot::refundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetCash = snapshots.stream().map(StoreSnapshot::netCash).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalMembers = snapshots.stream().mapToLong(StoreSnapshot::totalMembers).sum();
        long totalReservations = snapshots.stream().mapToLong(StoreSnapshot::totalReservations).sum();
        return new HqOverview(
                snapshots.size(),
                totalMembers,
                totalReservations,
                totalRevenue,
                totalRefund,
                totalNetCash
        );
    }

    public List<StoreRankingItem> ranking(String tenantId, List<String> storeIds) {
        return storeSnapshots(tenantId, storeIds).stream()
                .sorted(Comparator.comparing(StoreSnapshot::netCash).reversed())
                .map(snapshot -> new StoreRankingItem(
                        snapshot.storeId(),
                        snapshot.storeName(),
                        snapshot.paidAmount(),
                        snapshot.refundAmount(),
                        snapshot.netCash()
                ))
                .toList();
    }

    public List<TrendPoint> trend(String tenantId, List<String> storeIds, LocalDate startDate, int days) {
        LocalDate from = startDate == null ? LocalDate.now().minusDays(Math.max(1, days) - 1L) : startDate;
        int safeDays = Math.max(1, days);
        List<TrendPoint> points = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = from.plusDays(i);
            BigDecimal paid = BigDecimal.ZERO;
            BigDecimal refund = BigDecimal.ZERO;
            for (String storeId : storeIds) {
                var daily = financeService.dailyReconcile(tenantId, storeId, date);
                paid = paid.add(daily.actualAmount().add(daily.diffAmount()));
                refund = refund.add(daily.expectedAmount().subtract(daily.actualAmount()));
            }
            points.add(new TrendPoint(date, paid, refund, paid.subtract(refund)));
        }
        return points;
    }

    public StoreDrilldown drilldown(String tenantId, String storeId) {
        StoreSettings store = storeSettingsService.get(tenantId, storeId);
        return new StoreDrilldown(
                store.storeId(),
                store.storeName(),
                reportService.overview(tenantId, storeId),
                reportService.attendance(tenantId, storeId),
                reportService.finance(tenantId, storeId)
        );
    }

    public List<MetricDefinition> listMetricDefinitions() {
        return metricDefinitions.values().stream()
                .sorted(Comparator.comparing(MetricDefinition::metricKey))
                .toList();
    }

    @Transactional
    public MetricDefinition updateMetricDefinition(String metricKey, String displayName, String formula, String description) {
        if (metricKey == null || metricKey.isBlank()) {
            throw new IllegalArgumentException("指标编码不能为空");
        }
        MetricDefinition definition = new MetricDefinition(metricKey, displayName, formula, description, metricVersion.incrementAndGet());
        metricDefinitions.put(metricKey, definition);
        return definition;
    }

    private List<StoreSnapshot> storeSnapshots(String tenantId, List<String> storeIds) {
        Map<String, String> nameByStoreId = new LinkedHashMap<>();
        for (StoreSettings store : storeSettingsService.listByTenant(tenantId)) {
            nameByStoreId.put(store.storeId(), store.storeName());
        }
        List<StoreSnapshot> snapshots = new ArrayList<>();
        for (String storeId : storeIds) {
            if (storeId == null || storeId.isBlank()) {
                continue;
            }
            String storeName = nameByStoreId.getOrDefault(storeId, storeId);
            ReportService.OverviewReport overview = reportService.overview(tenantId, storeId);
            ReportService.FinanceReport finance = reportService.finance(tenantId, storeId);
            snapshots.add(new StoreSnapshot(
                    storeId,
                    storeName,
                    overview.totalMembers() == null ? 0 : overview.totalMembers(),
                    overview.totalReservations() == null ? 0 : overview.totalReservations(),
                    safe(finance.paidAmount()),
                    safe(finance.refundAmount()),
                    safe(finance.netCashAmount())
            ));
        }
        return snapshots;
    }

    private void initDefaultMetrics() {
        metricDefinitions.put("revenue", new MetricDefinition(
                "revenue",
                "总营收",
                "sum(paid_amount)",
                "已支付流水金额之和",
                metricVersion.get()
        ));
        metricDefinitions.put("refund_rate", new MetricDefinition(
                "refund_rate",
                "退款率",
                "sum(refund_amount)/sum(paid_amount)",
                "已审批退款金额占支付金额比例",
                metricVersion.get()
        ));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record StoreSnapshot(String storeId,
                                 String storeName,
                                 long totalMembers,
                                 long totalReservations,
                                 BigDecimal paidAmount,
                                 BigDecimal refundAmount,
                                 BigDecimal netCash) {
    }

    public record HqOverview(int storeCount,
                             long totalMembers,
                             long totalReservations,
                             BigDecimal totalRevenue,
                             BigDecimal totalRefund,
                             BigDecimal totalNetCash) {
    }

    public record StoreRankingItem(String storeId,
                                   String storeName,
                                   BigDecimal paidAmount,
                                   BigDecimal refundAmount,
                                   BigDecimal netCash) {
    }

    public record TrendPoint(LocalDate bizDate,
                             BigDecimal paidAmount,
                             BigDecimal refundAmount,
                             BigDecimal netCashAmount) {
    }

    public record StoreDrilldown(String storeId,
                                 String storeName,
                                 ReportService.OverviewReport overview,
                                 ReportService.AttendanceReport attendance,
                                 ReportService.FinanceReport finance) {
    }

    public record MetricDefinition(String metricKey,
                                   String displayName,
                                   String formula,
                                   String description,
                                   Long version) {
    }
}
