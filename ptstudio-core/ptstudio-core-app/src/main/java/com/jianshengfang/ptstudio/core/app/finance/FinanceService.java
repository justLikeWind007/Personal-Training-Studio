package com.jianshengfang.ptstudio.core.app.finance;

import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FinanceService {

    private final FinanceRepository financeRepository;
    private final MemberService memberService;

    public FinanceService(FinanceRepository financeRepository, MemberService memberService) {
        this.financeRepository = financeRepository;
        this.memberService = memberService;
    }

    @Transactional
    public InMemoryFinanceStore.OrderData createOrder(CreateOrderCommand command) {
        memberService.get(command.memberId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
        if (command.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于0");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String orderNo = "O" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return financeRepository.createOrder(
                command.tenantId(),
                command.storeId(),
                command.memberId(),
                command.orderType(),
                command.totalAmount(),
                orderNo,
                now
        );
    }

    public List<InMemoryFinanceStore.OrderData> listOrders(String tenantId, String storeId) {
        return financeRepository.listOrders(tenantId, storeId);
    }

    public Optional<InMemoryFinanceStore.OrderData> getOrder(Long orderId, String tenantId, String storeId) {
        return financeRepository.getOrder(orderId, tenantId, storeId);
    }

    @Transactional
    public InMemoryFinanceStore.PaymentData precreateAlipay(Long orderId, String tenantId, String storeId) {
        InMemoryFinanceStore.OrderData order = getOrder(orderId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!order.status().equals("CREATED")) {
            throw new IllegalArgumentException("当前订单状态不可发起支付");
        }

        OffsetDateTime now = OffsetDateTime.now();
        return financeRepository.createPrepayment(
                order.id(),
                tenantId,
                storeId,
                "P" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                "OUT" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                order.totalAmount(),
                now
        );
    }

    @Transactional
    public InMemoryFinanceStore.PaymentData alipayCallback(PaymentCallbackCommand command) {
        InMemoryFinanceStore.OrderData order = getOrder(command.orderId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        Optional<InMemoryFinanceStore.PaymentData> existing = financeRepository.getPaymentByChannelTradeNo(
                command.tenantId(), command.storeId(), command.channelTradeNo());
        if (existing.isPresent()) {
            return existing.get();
        }

        InMemoryFinanceStore.PaymentData payment = financeRepository
                .getLatestPaymentByOrder(order.id(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("请先发起预下单"));

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.PaymentData paid = financeRepository.markPaymentPaid(
                payment.id(),
                payment.tenantId(),
                payment.storeId(),
                command.channelTradeNo(),
                command.callbackRaw(),
                now
        );
        financeRepository.updateOrderPaid(
                order.id(),
                order.tenantId(),
                order.storeId(),
                paid.amount(),
                "PAID",
                now
        );
        return paid;
    }

    @Transactional
    public InMemoryFinanceStore.RefundData createRefund(CreateRefundCommand command) {
        InMemoryFinanceStore.OrderData order = getOrder(command.orderId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!order.status().equals("PAID")) {
            throw new IllegalArgumentException("仅已支付订单允许发起退款");
        }
        if (command.refundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额非法");
        }
        BigDecimal reservedAmount = financeRepository.sumReservedRefundAmountByOrder(
                order.id(), command.tenantId(), command.storeId());
        BigDecimal restRefundableAmount = order.totalAmount().subtract(reservedAmount);
        if (restRefundableAmount.compareTo(BigDecimal.ZERO) < 0
                || command.refundAmount().compareTo(restRefundableAmount) > 0) {
            throw new IllegalArgumentException("退款金额超过可退额度");
        }

        InMemoryFinanceStore.PaymentData paidPayment = financeRepository
                .getLatestPaidPaymentByOrder(order.id(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("未找到已支付流水"));

        OffsetDateTime now = OffsetDateTime.now();
        return financeRepository.createRefund(
                order.id(),
                paidPayment.id(),
                command.tenantId(),
                command.storeId(),
                command.refundAmount(),
                command.reason(),
                "RF" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                now
        );
    }

    @Transactional
    public InMemoryFinanceStore.RefundData approveRefund(Long refundId,
                                                         String tenantId,
                                                         String storeId,
                                                         Long approvedBy) {
        InMemoryFinanceStore.RefundData refund = financeRepository.getRefund(refundId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        if (!refund.status().equals("PENDING")) {
            throw new IllegalArgumentException("当前退款单状态不可审批");
        }

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.RefundData approved = financeRepository.updateRefundDecision(
                refund.id(),
                tenantId,
                storeId,
                "APPROVED",
                approvedBy,
                now
        );
        InMemoryFinanceStore.OrderData order = financeRepository.getOrder(refund.orderId(), tenantId, storeId).orElse(null);
        if (order != null) {
            BigDecimal approvedRefundAmount = financeRepository.sumApprovedRefundAmountByOrder(
                    order.id(), order.tenantId(), order.storeId());
            BigDecimal remainedPaidAmount = order.totalAmount().subtract(approvedRefundAmount);
            if (remainedPaidAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("退款累计金额异常");
            }
            String orderStatus = remainedPaidAmount.compareTo(BigDecimal.ZERO) == 0 ? "REFUNDED" : "PAID";
            financeRepository.updateOrderRefunded(
                    order.id(),
                    order.tenantId(),
                    order.storeId(),
                    remainedPaidAmount,
                    orderStatus,
                    now
            );
        }
        return approved;
    }

    @Transactional
    public InMemoryFinanceStore.RefundData rejectRefund(Long refundId,
                                                        String tenantId,
                                                        String storeId,
                                                        Long approvedBy) {
        InMemoryFinanceStore.RefundData refund = financeRepository.getRefund(refundId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        if (!refund.status().equals("PENDING")) {
            throw new IllegalArgumentException("当前退款单状态不可审批");
        }

        OffsetDateTime now = OffsetDateTime.now();
        return financeRepository.updateRefundDecision(
                refund.id(),
                tenantId,
                storeId,
                "REJECTED",
                approvedBy,
                now
        );
    }

    public InMemoryFinanceStore.DailyReconcileData dailyReconcile(String tenantId, String storeId, LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;

        BigDecimal paidAmount = financeRepository.sumPaidAmountByDate(tenantId, storeId, date);
        BigDecimal approvedRefundAmount = financeRepository.sumApprovedRefundAmountByDate(tenantId, storeId, date);

        BigDecimal actual = paidAmount.subtract(approvedRefundAmount);
        BigDecimal expected = actual;
        BigDecimal diff = expected.subtract(actual);
        String status = diff.compareTo(BigDecimal.ZERO) == 0 ? "BALANCED" : "DIFF_FOUND";

        return new InMemoryFinanceStore.DailyReconcileData(date, tenantId, storeId, expected, actual, diff, status);
    }

    public record CreateOrderCommand(String tenantId,
                                     String storeId,
                                     Long memberId,
                                     String orderType,
                                     BigDecimal totalAmount) {
    }

    public record PaymentCallbackCommand(String tenantId,
                                         String storeId,
                                         Long orderId,
                                         String channelTradeNo,
                                         String callbackRaw) {
    }

    public record CreateRefundCommand(String tenantId,
                                      String storeId,
                                      Long orderId,
                                      BigDecimal refundAmount,
                                      String reason) {
    }
}
