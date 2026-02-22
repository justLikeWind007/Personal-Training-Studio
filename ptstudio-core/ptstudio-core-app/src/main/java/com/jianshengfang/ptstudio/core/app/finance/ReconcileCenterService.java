package com.jianshengfang.ptstudio.core.app.finance;

import com.jianshengfang.ptstudio.core.app.attendance.AttendanceService;
import com.jianshengfang.ptstudio.core.app.commission.CommissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class ReconcileCenterService {

    private final FinanceService financeService;
    private final AttendanceService attendanceService;
    private final CommissionService commissionService;
    private final Map<String, IssueStatusSnapshot> issueStatusByNo = new ConcurrentHashMap<>();

    public ReconcileCenterService(FinanceService financeService,
                                  AttendanceService attendanceService,
                                  CommissionService commissionService) {
        this.financeService = financeService;
        this.attendanceService = attendanceService;
        this.commissionService = commissionService;
    }

    public ReconcileOverview overview(String tenantId, String storeId, LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        List<InMemoryFinanceStore.OrderData> orders = financeService.listOrders(tenantId, storeId);
        List<InMemoryFinanceStore.PaymentData> payments = financeService.listPayments(tenantId, storeId);
        List<InMemoryFinanceStore.RefundData> refunds = financeService.listRefunds(tenantId, storeId);
        List<com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore.ConsumptionData> consumptions =
                attendanceService.listConsumptions(tenantId, storeId);
        List<com.jianshengfang.ptstudio.core.app.commission.InMemoryCommissionStore.CommissionStatementData> statements =
                commissionService.listStatements(tenantId, storeId, null, null);

        BigDecimal paidAmount = payments.stream()
                .filter(payment -> "PAID".equals(payment.payStatus()))
                .filter(payment -> payment.paidAt() != null && date.equals(payment.paidAt().toLocalDate()))
                .map(InMemoryFinanceStore.PaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = refunds.stream()
                .filter(refund -> "APPROVED".equals(refund.status()))
                .filter(refund -> refund.approvedAt() != null && date.equals(refund.approvedAt().toLocalDate()))
                .map(InMemoryFinanceStore.RefundData::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lockedStatements = statements.stream().filter(statement -> "LOCKED".equals(statement.status())).count();
        long reversedConsumptions = consumptions.stream().filter(c -> "REVERSED".equals(c.status())).count();
        List<ReconcileIssue> issues = listIssues(tenantId, storeId, null, null);

        return new ReconcileOverview(
                date,
                orders.size(),
                paidAmount,
                refundAmount,
                consumptions.size(),
                reversedConsumptions,
                lockedStatements,
                issues.size()
        );
    }

    public List<ReconcileIssue> listIssues(String tenantId, String storeId, String status, String type) {
        List<ReconcileIssue> generated = generateIssues(tenantId, storeId);
        return generated.stream()
                .filter(issue -> status == null || issue.status().equals(status))
                .filter(issue -> type == null || issue.type().equals(type))
                .sorted(Comparator.comparing(ReconcileIssue::issueNo))
                .toList();
    }

    @Transactional
    public ReconcileIssue retryIssue(String tenantId, String storeId, String issueNo, Long operatorUserId) {
        ReconcileIssue existing = requireIssue(tenantId, storeId, issueNo);
        IssueStatusSnapshot snapshot = new IssueStatusSnapshot(
                "RETRYING",
                "RETRY",
                operatorUserId,
                OffsetDateTime.now()
        );
        issueStatusByNo.put(existing.issueNo(), snapshot);
        return applyStatus(existing, snapshot);
    }

    @Transactional
    public ReconcileIssue closeIssue(String tenantId, String storeId, String issueNo, Long operatorUserId) {
        ReconcileIssue existing = requireIssue(tenantId, storeId, issueNo);
        IssueStatusSnapshot snapshot = new IssueStatusSnapshot(
                "CLOSED",
                "MANUAL_CLOSE",
                operatorUserId,
                OffsetDateTime.now()
        );
        issueStatusByNo.put(existing.issueNo(), snapshot);
        return applyStatus(existing, snapshot);
    }

    public String exportIssuesCsv(String tenantId, String storeId, String status, String type) {
        List<ReconcileIssue> issues = listIssues(tenantId, storeId, status, type);
        StringBuilder csv = new StringBuilder("issueNo,type,severity,status,bizType,bizId,detail,lastAction,updatedAt\n");
        for (ReconcileIssue issue : issues) {
            csv.append(escape(issue.issueNo())).append(',')
                    .append(escape(issue.type())).append(',')
                    .append(escape(issue.severity())).append(',')
                    .append(escape(issue.status())).append(',')
                    .append(escape(issue.bizType())).append(',')
                    .append(issue.bizId()).append(',')
                    .append(escape(issue.detail())).append(',')
                    .append(escape(issue.lastAction())).append(',')
                    .append(escape(issue.updatedAt() == null ? "" : issue.updatedAt().toString()))
                    .append('\n');
        }
        return csv.toString();
    }

    private ReconcileIssue requireIssue(String tenantId, String storeId, String issueNo) {
        return listIssues(tenantId, storeId, null, null).stream()
                .filter(issue -> issue.issueNo().equals(issueNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("稽核异常不存在"));
    }

    private List<ReconcileIssue> generateIssues(String tenantId, String storeId) {
        List<ReconcileIssue> issues = new ArrayList<>();
        List<InMemoryFinanceStore.OrderData> orders = financeService.listOrders(tenantId, storeId);
        List<InMemoryFinanceStore.PaymentData> payments = financeService.listPayments(tenantId, storeId);
        List<InMemoryFinanceStore.RefundData> refunds = financeService.listRefunds(tenantId, storeId);
        List<com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore.ConsumptionData> consumptions =
                attendanceService.listConsumptions(tenantId, storeId);

        for (InMemoryFinanceStore.RefundData refund : refunds) {
            if ("PENDING".equals(refund.status())) {
                issues.add(issue(
                        "REFUND_PENDING_" + refund.id(),
                        "REFUND_PENDING",
                        "HIGH",
                        "REFUND",
                        refund.id(),
                        "退款单待审批，请尽快处理",
                        refund.updatedAt()
                ));
            }
        }

        for (InMemoryFinanceStore.PaymentData payment : payments) {
            if ("WAIT_PAY".equals(payment.payStatus())) {
                issues.add(issue(
                        "PAYMENT_WAIT_" + payment.id(),
                        "PAYMENT_WAIT_PAY",
                        "MEDIUM",
                        "PAYMENT",
                        payment.id(),
                        "支付流水待回调确认",
                        payment.updatedAt()
                ));
            }
        }

        for (InMemoryFinanceStore.OrderData order : orders) {
            BigDecimal reservedRefundAmount = refunds.stream()
                    .filter(refund -> refund.orderId().equals(order.id()))
                    .filter(refund -> "PENDING".equals(refund.status()) || "APPROVED".equals(refund.status()))
                    .map(InMemoryFinanceStore.RefundData::refundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (reservedRefundAmount.compareTo(order.totalAmount()) > 0) {
                issues.add(issue(
                        "REFUND_OVER_LIMIT_" + order.id(),
                        "REFUND_OVER_LIMIT",
                        "HIGH",
                        "ORDER",
                        order.id(),
                        "订单退款累计金额超过订单总额",
                        order.updatedAt()
                ));
            }
        }

        for (com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore.ConsumptionData consumption : consumptions) {
            if ("REVERSED".equals(consumption.status())) {
                issues.add(issue(
                        "CONSUMPTION_REVERSED_" + consumption.id(),
                        "CONSUMPTION_REVERSED",
                        "LOW",
                        "CONSUMPTION",
                        consumption.id(),
                        "课消已冲正，需复核业务原因",
                        consumption.updatedAt()
                ));
            }
        }
        return issues;
    }

    private ReconcileIssue issue(String issueNo,
                                 String type,
                                 String severity,
                                 String bizType,
                                 Long bizId,
                                 String detail,
                                 OffsetDateTime updatedAt) {
        ReconcileIssue created = new ReconcileIssue(
                issueNo,
                type,
                severity,
                "NEW",
                bizType,
                bizId,
                detail,
                "NONE",
                updatedAt
        );
        IssueStatusSnapshot snapshot = issueStatusByNo.get(issueNo);
        if (snapshot == null) {
            return created;
        }
        return applyStatus(created, snapshot);
    }

    private ReconcileIssue applyStatus(ReconcileIssue issue, IssueStatusSnapshot snapshot) {
        String action = snapshot.action() + "(operator=" + snapshot.operatorUserId() + ")";
        return new ReconcileIssue(
                issue.issueNo(),
                issue.type(),
                issue.severity(),
                snapshot.status(),
                issue.bizType(),
                issue.bizId(),
                issue.detail(),
                action,
                snapshot.updatedAt()
        );
    }

    private String escape(String source) {
        if (source == null) {
            return "";
        }
        return "\"" + source.replace("\"", "\"\"") + "\"";
    }

    public record ReconcileOverview(LocalDate bizDate,
                                    int orderCount,
                                    BigDecimal paidAmount,
                                    BigDecimal refundAmount,
                                    int consumptionCount,
                                    long reversedConsumptionCount,
                                    long lockedCommissionCount,
                                    int anomalyCount) {
    }

    public record ReconcileIssue(String issueNo,
                                 String type,
                                 String severity,
                                 String status,
                                 String bizType,
                                 Long bizId,
                                 String detail,
                                 String lastAction,
                                 OffsetDateTime updatedAt) {
    }

    private record IssueStatusSnapshot(String status,
                                       String action,
                                       Long operatorUserId,
                                       OffsetDateTime updatedAt) {
    }
}
