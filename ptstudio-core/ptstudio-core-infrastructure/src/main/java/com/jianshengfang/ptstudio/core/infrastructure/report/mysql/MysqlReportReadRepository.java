package com.jianshengfang.ptstudio.core.infrastructure.report.mysql;

import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import com.jianshengfang.ptstudio.core.app.report.ReportReadRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@Profile("mysql")
public class MysqlReportReadRepository implements ReportReadRepository {

    private final MysqlReportMapper mapper;
    private final InMemoryAttendanceStore attendanceStore;

    public MysqlReportReadRepository(MysqlReportMapper mapper, InMemoryAttendanceStore attendanceStore) {
        this.mapper = mapper;
        this.attendanceStore = attendanceStore;
    }

    @Override
    public long countDistinctMembers(String tenantId, String storeId) {
        Long count = mapper.countDistinctMembers(toLong(tenantId), toLong(storeId));
        return count == null ? 0L : count;
    }

    @Override
    public long countReservations(String tenantId, String storeId) {
        Long count = mapper.countReservations(tenantId, storeId);
        return count == null ? 0L : count;
    }

    @Override
    public long countCanceledReservations(String tenantId, String storeId) {
        Long count = mapper.countCanceledReservations(tenantId, storeId);
        return count == null ? 0L : count;
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
        BigDecimal sum = mapper.sumPaidRevenue(toLong(tenantId), toLong(storeId));
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumOrderAmount(String tenantId, String storeId) {
        BigDecimal sum = mapper.sumOrderAmount(toLong(tenantId), toLong(storeId));
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumApprovedRefundAmount(String tenantId, String storeId) {
        BigDecimal sum = mapper.sumApprovedRefundAmount(toLong(tenantId), toLong(storeId));
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumCommissionAmount(String tenantId, String storeId) {
        BigDecimal sum = mapper.sumCommissionAmount(toLong(tenantId), toLong(storeId));
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private long toLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1L;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 1L;
        }
        return Long.parseLong(digits);
    }
}
