package com.jianshengfang.ptstudio.core.app.ops;

import com.jianshengfang.ptstudio.core.app.attendance.AttendanceService;
import com.jianshengfang.ptstudio.core.app.finance.FinanceService;
import com.jianshengfang.ptstudio.core.app.finance.InMemoryFinanceStore;
import com.jianshengfang.ptstudio.core.app.report.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class RiskAlertService {

    private final FinanceService financeService;
    private final AttendanceService attendanceService;
    private final ReportService reportService;
    private final Map<String, AlertStatusSnapshot> alertStatusByNo = new ConcurrentHashMap<>();

    public RiskAlertService(FinanceService financeService,
                            AttendanceService attendanceService,
                            ReportService reportService) {
        this.financeService = financeService;
        this.attendanceService = attendanceService;
        this.reportService = reportService;
    }

    public List<RiskAlert> list(String tenantId, String storeId, String status, String type) {
        return generateAlerts(tenantId, storeId).stream()
                .filter(alert -> status == null || status.equals(alert.status()))
                .filter(alert -> type == null || type.equals(alert.type()))
                .sorted(Comparator.comparing(RiskAlert::alertNo))
                .toList();
    }

    @Transactional
    public RiskAlert ack(String tenantId, String storeId, String alertNo, Long operatorUserId) {
        RiskAlert existing = requireAlert(tenantId, storeId, alertNo);
        AlertStatusSnapshot snapshot = new AlertStatusSnapshot(
                "ACK",
                "ACK",
                operatorUserId,
                OffsetDateTime.now()
        );
        alertStatusByNo.put(existing.alertNo(), snapshot);
        return applyStatus(existing, snapshot);
    }

    @Transactional
    public RiskAlert close(String tenantId, String storeId, String alertNo, Long operatorUserId) {
        RiskAlert existing = requireAlert(tenantId, storeId, alertNo);
        AlertStatusSnapshot snapshot = new AlertStatusSnapshot(
                "CLOSED",
                "CLOSE",
                operatorUserId,
                OffsetDateTime.now()
        );
        alertStatusByNo.put(existing.alertNo(), snapshot);
        return applyStatus(existing, snapshot);
    }

    public String exportCsv(String tenantId, String storeId, String status, String type) {
        List<RiskAlert> alerts = list(tenantId, storeId, status, type);
        StringBuilder csv = new StringBuilder("alertNo,type,severity,status,bizType,bizId,detail,lastAction,handledBy,handledAt,updatedAt\\n");
        for (RiskAlert alert : alerts) {
            csv.append(escape(alert.alertNo())).append(',')
                    .append(escape(alert.type())).append(',')
                    .append(escape(alert.severity())).append(',')
                    .append(escape(alert.status())).append(',')
                    .append(escape(alert.bizType())).append(',')
                    .append(alert.bizId() == null ? "" : alert.bizId()).append(',')
                    .append(escape(alert.detail())).append(',')
                    .append(escape(alert.lastAction())).append(',')
                    .append(alert.handledBy() == null ? "" : alert.handledBy()).append(',')
                    .append(escape(alert.handledAt() == null ? "" : alert.handledAt().toString())).append(',')
                    .append(escape(alert.updatedAt() == null ? "" : alert.updatedAt().toString()))
                    .append('\n');
        }
        return csv.toString();
    }

    private RiskAlert requireAlert(String tenantId, String storeId, String alertNo) {
        return list(tenantId, storeId, null, null).stream()
                .filter(alert -> alert.alertNo().equals(alertNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("风险预警不存在"));
    }

    private List<RiskAlert> generateAlerts(String tenantId, String storeId) {
        List<RiskAlert> alerts = new ArrayList<>();

        List<InMemoryFinanceStore.OrderData> orders = financeService.listOrders(tenantId, storeId);
        Map<Long, InMemoryFinanceStore.OrderData> orderById = new HashMap<>();
        for (InMemoryFinanceStore.OrderData order : orders) {
            orderById.put(order.id(), order);
        }

        for (InMemoryFinanceStore.RefundData refund : financeService.listRefunds(tenantId, storeId)) {
            if ("PENDING".equals(refund.status())) {
                alerts.add(baseAlert(
                        "RISK_REFUND_PENDING_" + refund.id(),
                        "REFUND_PENDING",
                        "HIGH",
                        "REFUND",
                        refund.id(),
                        "退款单待处理，存在资金风险",
                        refund.updatedAt()
                ));
            }

            InMemoryFinanceStore.OrderData order = orderById.get(refund.orderId());
            if (order != null && order.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = refund.refundAmount()
                        .divide(order.totalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (ratio.compareTo(BigDecimal.valueOf(50)) >= 0) {
                    alerts.add(baseAlert(
                            "RISK_REFUND_RATIO_" + refund.id(),
                            "REFUND_HIGH_RATIO",
                            "HIGH",
                            "REFUND",
                            refund.id(),
                            "退款金额占订单比例达到 " + ratio.setScale(2, RoundingMode.HALF_UP) + "%",
                            refund.updatedAt()
                    ));
                }
            }
        }

        for (com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore.ConsumptionData consumption
                : attendanceService.listConsumptions(tenantId, storeId)) {
            if ("REVERSED".equals(consumption.status())) {
                alerts.add(baseAlert(
                        "RISK_CONSUMPTION_REVERSED_" + consumption.id(),
                        "CONSUMPTION_REVERSED",
                        "MEDIUM",
                        "CONSUMPTION",
                        consumption.id(),
                        "课消记录发生冲正，需复核业务原因",
                        consumption.updatedAt()
                ));
            }
        }

        ReportService.AttendanceReport attendance = reportService.attendance(tenantId, storeId);
        if (attendance.totalReservations() >= 3 && attendance.attendanceRatePercent().compareTo(BigDecimal.valueOf(60)) < 0) {
            alerts.add(baseAlert(
                    "RISK_ATTENDANCE_LOW_" + storeId,
                    "ATTENDANCE_LOW",
                    "MEDIUM",
                    "STORE",
                    null,
                    "到店率低于60%，当前到店率=" + attendance.attendanceRatePercent() + "%",
                    OffsetDateTime.now()
            ));
        }

        return alerts;
    }

    private RiskAlert baseAlert(String alertNo,
                                String type,
                                String severity,
                                String bizType,
                                Long bizId,
                                String detail,
                                OffsetDateTime updatedAt) {
        RiskAlert created = new RiskAlert(
                alertNo,
                type,
                severity,
                "NEW",
                bizType,
                bizId,
                detail,
                "NONE",
                null,
                null,
                updatedAt
        );
        AlertStatusSnapshot snapshot = alertStatusByNo.get(alertNo);
        if (snapshot == null) {
            return created;
        }
        return applyStatus(created, snapshot);
    }

    private RiskAlert applyStatus(RiskAlert alert, AlertStatusSnapshot snapshot) {
        return new RiskAlert(
                alert.alertNo(),
                alert.type(),
                alert.severity(),
                snapshot.status(),
                alert.bizType(),
                alert.bizId(),
                alert.detail(),
                snapshot.action(),
                snapshot.operatorUserId(),
                snapshot.updatedAt(),
                snapshot.updatedAt()
        );
    }

    private String escape(String source) {
        if (source == null) {
            return "";
        }
        return "\"" + source.replace("\"", "\"\"") + "\"";
    }

    public record RiskAlert(String alertNo,
                            String type,
                            String severity,
                            String status,
                            String bizType,
                            Long bizId,
                            String detail,
                            String lastAction,
                            Long handledBy,
                            OffsetDateTime handledAt,
                            OffsetDateTime updatedAt) {
    }

    private record AlertStatusSnapshot(String status,
                                       String action,
                                       Long operatorUserId,
                                       OffsetDateTime updatedAt) {
    }
}
