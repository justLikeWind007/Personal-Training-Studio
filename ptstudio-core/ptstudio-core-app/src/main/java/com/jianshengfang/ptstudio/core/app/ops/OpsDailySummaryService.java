package com.jianshengfang.ptstudio.core.app.ops;

import com.jianshengfang.ptstudio.core.app.attendance.AttendanceService;
import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import com.jianshengfang.ptstudio.core.app.finance.FinanceService;
import com.jianshengfang.ptstudio.core.app.report.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class OpsDailySummaryService {

    private final FinanceService financeService;
    private final MemberService memberService;
    private final AttendanceService attendanceService;
    private final ReportService reportService;

    public OpsDailySummaryService(FinanceService financeService,
                                  MemberService memberService,
                                  AttendanceService attendanceService,
                                  ReportService reportService) {
        this.financeService = financeService;
        this.memberService = memberService;
        this.attendanceService = attendanceService;
        this.reportService = reportService;
    }

    public DailySummary summary(String tenantId, String storeId, LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now().minusDays(1) : bizDate;
        long newMembers = memberService.list(tenantId, storeId).stream()
                .filter(member -> member.joinDate() != null && date.equals(member.joinDate().toLocalDate()))
                .count();
        long orderCount = financeService.listOrders(tenantId, storeId).stream()
                .filter(order -> order.createdAt() != null && date.equals(order.createdAt().toLocalDate()))
                .count();
        BigDecimal actualAmount = financeService.dailyReconcile(tenantId, storeId, date).actualAmount();
        BigDecimal refundAmount = financeService.listRefunds(tenantId, storeId).stream()
                .filter(refund -> "APPROVED".equals(refund.status()))
                .filter(refund -> refund.approvedAt() != null && date.equals(refund.approvedAt().toLocalDate()))
                .map(refund -> refund.refundAmount() == null ? BigDecimal.ZERO : refund.refundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long consumptionCount = attendanceService.listConsumptions(tenantId, storeId).stream()
                .filter(consumption -> "CONSUMED".equals(consumption.status()))
                .filter(consumption -> consumption.consumeTime() != null && date.equals(consumption.consumeTime().toLocalDate()))
                .count();
        BigDecimal attendanceRate = reportService.attendance(tenantId, storeId).attendanceRatePercent();
        return new DailySummary(date, newMembers, orderCount, actualAmount, refundAmount, consumptionCount, attendanceRate);
    }

    public String text(DailySummary summary, String storeId) {
        return "【运营日报】%s 门店=%s 新增会员=%d 订单=%d 实收=%s 退款=%s 课消=%d 到课率=%s%%"
                .formatted(
                        summary.bizDate(),
                        storeId,
                        summary.newMembers(),
                        summary.orderCount(),
                        summary.actualAmount(),
                        summary.refundAmount(),
                        summary.consumptionCount(),
                        summary.attendanceRatePercent()
                );
    }

    public record DailySummary(LocalDate bizDate,
                               long newMembers,
                               long orderCount,
                               BigDecimal actualAmount,
                               BigDecimal refundAmount,
                               long consumptionCount,
                               BigDecimal attendanceRatePercent) {
    }
}
