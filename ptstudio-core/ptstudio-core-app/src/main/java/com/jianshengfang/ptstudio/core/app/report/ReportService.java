package com.jianshengfang.ptstudio.core.app.report;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReportService {

    private final ReportReadRepository reportReadRepository;

    public ReportService(ReportReadRepository reportReadRepository) {
        this.reportReadRepository = reportReadRepository;
    }

    public OverviewReport overview(String tenantId, String storeId) {
        long totalMembers = reportReadRepository.countDistinctMembers(tenantId, storeId);
        long totalReservations = reportReadRepository.countReservations(tenantId, storeId);
        long totalCheckins = reportReadRepository.countCheckins(tenantId, storeId);
        BigDecimal totalRevenue = reportReadRepository.sumPaidRevenue(tenantId, storeId);

        return new OverviewReport(totalMembers, totalReservations, totalCheckins, totalRevenue);
    }

    public AttendanceReport attendance(String tenantId, String storeId) {
        long bookedReservations = reportReadRepository.countReservations(tenantId, storeId);
        long canceledReservations = reportReadRepository.countCanceledReservations(tenantId, storeId);
        long checkins = reportReadRepository.countCheckins(tenantId, storeId);
        long consumptions = reportReadRepository.countConsumedRecords(tenantId, storeId);

        BigDecimal attendanceRate = bookedReservations == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(checkins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(bookedReservations), 2, java.math.RoundingMode.HALF_UP);

        return new AttendanceReport(bookedReservations, canceledReservations, checkins, consumptions, attendanceRate);
    }

    public FinanceReport finance(String tenantId, String storeId) {
        BigDecimal orderAmount = reportReadRepository.sumOrderAmount(tenantId, storeId);
        BigDecimal paidAmount = reportReadRepository.sumPaidRevenue(tenantId, storeId);
        BigDecimal refundAmount = reportReadRepository.sumApprovedRefundAmount(tenantId, storeId);
        BigDecimal commissionAmount = reportReadRepository.sumCommissionAmount(tenantId, storeId);

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
