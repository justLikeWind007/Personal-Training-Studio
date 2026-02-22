package com.jianshengfang.ptstudio.core.app.report;

import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import com.jianshengfang.ptstudio.core.app.commission.InMemoryCommissionStore;
import com.jianshengfang.ptstudio.core.app.finance.InMemoryFinanceStore;
import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReportService {

    private final InMemoryScheduleStore scheduleStore;
    private final InMemoryAttendanceStore attendanceStore;
    private final InMemoryFinanceStore financeStore;
    private final InMemoryCommissionStore commissionStore;

    public ReportService(InMemoryScheduleStore scheduleStore,
                         InMemoryAttendanceStore attendanceStore,
                         InMemoryFinanceStore financeStore,
                         InMemoryCommissionStore commissionStore) {
        this.scheduleStore = scheduleStore;
        this.attendanceStore = attendanceStore;
        this.financeStore = financeStore;
        this.commissionStore = commissionStore;
    }

    public OverviewReport overview(String tenantId, String storeId) {
        long totalMembers = financeStore.orderById().values().stream()
                .filter(order -> order.tenantId().equals(tenantId) && order.storeId().equals(storeId))
                .map(InMemoryFinanceStore.OrderData::memberId)
                .distinct()
                .count();

        long totalReservations = scheduleStore.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .count();

        long totalCheckins = attendanceStore.checkinById().values().stream()
                .filter(c -> c.tenantId().equals(tenantId) && c.storeId().equals(storeId))
                .count();

        BigDecimal totalRevenue = financeStore.paymentById().values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.storeId().equals(storeId))
                .filter(p -> "PAID".equals(p.payStatus()))
                .map(InMemoryFinanceStore.PaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OverviewReport(totalMembers, totalReservations, totalCheckins, totalRevenue);
    }

    public AttendanceReport attendance(String tenantId, String storeId) {
        long bookedReservations = scheduleStore.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "BOOKED".equals(r.status()) || "CANCELED".equals(r.status()))
                .count();

        long canceledReservations = scheduleStore.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "CANCELED".equals(r.status()))
                .count();

        long checkins = attendanceStore.checkinById().values().stream()
                .filter(c -> c.tenantId().equals(tenantId) && c.storeId().equals(storeId))
                .count();

        long consumptions = attendanceStore.consumptionById().values().stream()
                .filter(c -> c.tenantId().equals(tenantId) && c.storeId().equals(storeId))
                .filter(c -> "CONSUMED".equals(c.status()))
                .count();

        BigDecimal attendanceRate = bookedReservations == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(checkins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(bookedReservations), 2, java.math.RoundingMode.HALF_UP);

        return new AttendanceReport(bookedReservations, canceledReservations, checkins, consumptions, attendanceRate);
    }

    public FinanceReport finance(String tenantId, String storeId) {
        BigDecimal orderAmount = financeStore.orderById().values().stream()
                .filter(o -> o.tenantId().equals(tenantId) && o.storeId().equals(storeId))
                .map(InMemoryFinanceStore.OrderData::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = financeStore.paymentById().values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.storeId().equals(storeId))
                .filter(p -> "PAID".equals(p.payStatus()))
                .map(InMemoryFinanceStore.PaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundAmount = financeStore.refundById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "APPROVED".equals(r.status()))
                .map(InMemoryFinanceStore.RefundData::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commissionAmount = commissionStore.statementById().values().stream()
                .filter(s -> s.tenantId().equals(tenantId) && s.storeId().equals(storeId))
                .map(InMemoryCommissionStore.CommissionStatementData::commissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCash = paidAmount.subtract(refundAmount);

        return new FinanceReport(orderAmount, paidAmount, refundAmount, netCash, commissionAmount);
    }

    public record OverviewReport(Long totalMembers,
                                 Long totalReservations,
                                 Long totalCheckins,
                                 BigDecimal totalRevenue) {
    }

    public record AttendanceReport(Long totalReservations,
                                   Long canceledReservations,
                                   Long totalCheckins,
                                   Long totalConsumptions,
                                   BigDecimal attendanceRatePercent) {
    }

    public record FinanceReport(BigDecimal orderAmount,
                                BigDecimal paidAmount,
                                BigDecimal refundAmount,
                                BigDecimal netCashAmount,
                                BigDecimal commissionAmount) {
    }
}
