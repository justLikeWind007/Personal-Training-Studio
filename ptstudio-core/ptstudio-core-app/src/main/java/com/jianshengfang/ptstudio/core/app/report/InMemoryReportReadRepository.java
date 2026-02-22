package com.jianshengfang.ptstudio.core.app.report;

import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import com.jianshengfang.ptstudio.core.app.commission.InMemoryCommissionStore;
import com.jianshengfang.ptstudio.core.app.finance.InMemoryFinanceStore;
import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@Profile("!mysql")
public class InMemoryReportReadRepository implements ReportReadRepository {

    private final InMemoryScheduleStore scheduleStore;
    private final InMemoryAttendanceStore attendanceStore;
    private final InMemoryFinanceStore financeStore;
    private final InMemoryCommissionStore commissionStore;

    public InMemoryReportReadRepository(InMemoryScheduleStore scheduleStore,
                                        InMemoryAttendanceStore attendanceStore,
                                        InMemoryFinanceStore financeStore,
                                        InMemoryCommissionStore commissionStore) {
        this.scheduleStore = scheduleStore;
        this.attendanceStore = attendanceStore;
        this.financeStore = financeStore;
        this.commissionStore = commissionStore;
    }

    @Override
    public long countDistinctMembers(String tenantId, String storeId) {
        return financeStore.orderById().values().stream()
                .filter(order -> order.tenantId().equals(tenantId) && order.storeId().equals(storeId))
                .map(InMemoryFinanceStore.OrderData::memberId)
                .distinct()
                .count();
    }

    @Override
    public long countReservations(String tenantId, String storeId) {
        return scheduleStore.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .count();
    }

    @Override
    public long countCanceledReservations(String tenantId, String storeId) {
        return scheduleStore.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "CANCELED".equals(r.status()))
                .count();
    }

    @Override
    public long countCheckins(String tenantId, String storeId) {
        return attendanceStore.checkinById().values().stream()
                .filter(c -> c.tenantId().equals(tenantId) && c.storeId().equals(storeId))
                .count();
    }

    @Override
    public long countConsumedRecords(String tenantId, String storeId) {
        return attendanceStore.consumptionById().values().stream()
                .filter(c -> c.tenantId().equals(tenantId) && c.storeId().equals(storeId))
                .filter(c -> "CONSUMED".equals(c.status()))
                .count();
    }

    @Override
    public BigDecimal sumPaidRevenue(String tenantId, String storeId) {
        return financeStore.paymentById().values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.storeId().equals(storeId))
                .filter(p -> "PAID".equals(p.payStatus()))
                .map(InMemoryFinanceStore.PaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumOrderAmount(String tenantId, String storeId) {
        return financeStore.orderById().values().stream()
                .filter(o -> o.tenantId().equals(tenantId) && o.storeId().equals(storeId))
                .map(InMemoryFinanceStore.OrderData::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumApprovedRefundAmount(String tenantId, String storeId) {
        return financeStore.refundById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "APPROVED".equals(r.status()))
                .map(InMemoryFinanceStore.RefundData::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumCommissionAmount(String tenantId, String storeId) {
        return commissionStore.statementById().values().stream()
                .filter(s -> s.tenantId().equals(tenantId) && s.storeId().equals(storeId))
                .map(InMemoryCommissionStore.CommissionStatementData::commissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
