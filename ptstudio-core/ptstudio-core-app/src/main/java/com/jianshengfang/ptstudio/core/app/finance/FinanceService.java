package com.jianshengfang.ptstudio.core.app.finance;

import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FinanceService {

    private final InMemoryFinanceStore store;
    private final MemberService memberService;

    public FinanceService(InMemoryFinanceStore store, MemberService memberService) {
        this.store = store;
        this.memberService = memberService;
    }

    public InMemoryFinanceStore.OrderData createOrder(CreateOrderCommand command) {
        memberService.get(command.memberId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
        if (command.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于0");
        }

        long id = store.nextOrderId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.OrderData order = new InMemoryFinanceStore.OrderData(
                id,
                String.format("O%08d", id),
                command.memberId(),
                command.tenantId(),
                command.storeId(),
                command.orderType(),
                command.totalAmount(),
                BigDecimal.ZERO,
                "CREATED",
                now,
                now
        );
        store.orderById().put(id, order);
        return order;
    }

    public List<InMemoryFinanceStore.OrderData> listOrders(String tenantId, String storeId) {
        return store.orderById().values().stream()
                .filter(order -> order.tenantId().equals(tenantId) && order.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryFinanceStore.OrderData::id))
                .toList();
    }

    public Optional<InMemoryFinanceStore.OrderData> getOrder(Long orderId, String tenantId, String storeId) {
        InMemoryFinanceStore.OrderData order = store.orderById().get(orderId);
        if (order == null) {
            return Optional.empty();
        }
        if (!order.tenantId().equals(tenantId) || !order.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(order);
    }

    public InMemoryFinanceStore.PaymentData precreateAlipay(Long orderId, String tenantId, String storeId) {
        InMemoryFinanceStore.OrderData order = getOrder(orderId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!order.status().equals("CREATED")) {
            throw new IllegalArgumentException("当前订单状态不可发起支付");
        }

        long paymentId = store.nextPaymentId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.PaymentData payment = new InMemoryFinanceStore.PaymentData(
                paymentId,
                order.id(),
                tenantId,
                storeId,
                "ALIPAY",
                null,
                order.totalAmount(),
                "WAIT_PAY",
                null,
                null,
                now,
                now
        );
        store.paymentById().put(paymentId, payment);
        return payment;
    }

    public InMemoryFinanceStore.PaymentData alipayCallback(PaymentCallbackCommand command) {
        InMemoryFinanceStore.OrderData order = getOrder(command.orderId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        Long existingPaymentId = store.paymentIdByTradeNo().get(command.channelTradeNo());
        if (existingPaymentId != null) {
            InMemoryFinanceStore.PaymentData existing = store.paymentById().get(existingPaymentId);
            if (existing != null) {
                return existing;
            }
        }

        InMemoryFinanceStore.PaymentData payment = store.paymentById().values().stream()
                .filter(p -> p.orderId().equals(order.id())
                        && p.tenantId().equals(command.tenantId())
                        && p.storeId().equals(command.storeId()))
                .max(Comparator.comparing(InMemoryFinanceStore.PaymentData::id))
                .orElseThrow(() -> new IllegalArgumentException("请先发起预下单"));

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.PaymentData paid = new InMemoryFinanceStore.PaymentData(
                payment.id(),
                payment.orderId(),
                payment.tenantId(),
                payment.storeId(),
                payment.payChannel(),
                command.channelTradeNo(),
                payment.amount(),
                "PAID",
                command.callbackRaw(),
                now,
                payment.createdAt(),
                now
        );
        store.paymentById().put(paid.id(), paid);
        store.paymentIdByTradeNo().put(command.channelTradeNo(), paid.id());

        InMemoryFinanceStore.OrderData paidOrder = new InMemoryFinanceStore.OrderData(
                order.id(),
                order.orderNo(),
                order.memberId(),
                order.tenantId(),
                order.storeId(),
                order.orderType(),
                order.totalAmount(),
                paid.amount(),
                "PAID",
                order.createdAt(),
                now
        );
        store.orderById().put(order.id(), paidOrder);
        return paid;
    }

    public InMemoryFinanceStore.RefundData createRefund(CreateRefundCommand command) {
        InMemoryFinanceStore.OrderData order = getOrder(command.orderId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!order.status().equals("PAID")) {
            throw new IllegalArgumentException("仅已支付订单允许发起退款");
        }
        if (command.refundAmount().compareTo(BigDecimal.ZERO) <= 0
                || command.refundAmount().compareTo(order.paidAmount()) > 0) {
            throw new IllegalArgumentException("退款金额非法");
        }

        long id = store.nextRefundId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.RefundData refund = new InMemoryFinanceStore.RefundData(
                id,
                String.format("RF%08d", id),
                order.id(),
                command.tenantId(),
                command.storeId(),
                command.refundAmount(),
                command.reason(),
                "PENDING",
                null,
                null,
                now,
                now
        );
        store.refundById().put(id, refund);
        return refund;
    }

    public InMemoryFinanceStore.RefundData approveRefund(Long refundId,
                                                         String tenantId,
                                                         String storeId,
                                                         Long approvedBy) {
        InMemoryFinanceStore.RefundData refund = getRefund(refundId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        if (!refund.status().equals("PENDING")) {
            throw new IllegalArgumentException("当前退款单状态不可审批");
        }

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.RefundData approved = new InMemoryFinanceStore.RefundData(
                refund.id(),
                refund.refundNo(),
                refund.orderId(),
                refund.tenantId(),
                refund.storeId(),
                refund.refundAmount(),
                refund.reason(),
                "APPROVED",
                approvedBy,
                now,
                refund.createdAt(),
                now
        );
        store.refundById().put(approved.id(), approved);

        InMemoryFinanceStore.OrderData order = store.orderById().get(refund.orderId());
        if (order != null) {
            InMemoryFinanceStore.OrderData refundedOrder = new InMemoryFinanceStore.OrderData(
                    order.id(),
                    order.orderNo(),
                    order.memberId(),
                    order.tenantId(),
                    order.storeId(),
                    order.orderType(),
                    order.totalAmount(),
                    order.paidAmount().subtract(approved.refundAmount()),
                    "REFUNDED",
                    order.createdAt(),
                    now
            );
            store.orderById().put(order.id(), refundedOrder);
        }
        return approved;
    }

    public InMemoryFinanceStore.RefundData rejectRefund(Long refundId,
                                                        String tenantId,
                                                        String storeId,
                                                        Long approvedBy) {
        InMemoryFinanceStore.RefundData refund = getRefund(refundId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        if (!refund.status().equals("PENDING")) {
            throw new IllegalArgumentException("当前退款单状态不可审批");
        }

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryFinanceStore.RefundData rejected = new InMemoryFinanceStore.RefundData(
                refund.id(),
                refund.refundNo(),
                refund.orderId(),
                refund.tenantId(),
                refund.storeId(),
                refund.refundAmount(),
                refund.reason(),
                "REJECTED",
                approvedBy,
                now,
                refund.createdAt(),
                now
        );
        store.refundById().put(rejected.id(), rejected);
        return rejected;
    }

    public InMemoryFinanceStore.DailyReconcileData dailyReconcile(String tenantId, String storeId, LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;

        BigDecimal paidAmount = store.paymentById().values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.storeId().equals(storeId))
                .filter(p -> "PAID".equals(p.payStatus()))
                .filter(p -> p.paidAt() != null && p.paidAt().toLocalDate().equals(date))
                .map(InMemoryFinanceStore.PaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedRefundAmount = store.refundById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "APPROVED".equals(r.status()))
                .filter(r -> r.approvedAt() != null && r.approvedAt().toLocalDate().equals(date))
                .map(InMemoryFinanceStore.RefundData::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actual = paidAmount.subtract(approvedRefundAmount);
        BigDecimal expected = actual;
        BigDecimal diff = expected.subtract(actual);
        String status = diff.compareTo(BigDecimal.ZERO) == 0 ? "BALANCED" : "DIFF_FOUND";

        return new InMemoryFinanceStore.DailyReconcileData(date, tenantId, storeId, expected, actual, diff, status);
    }

    private Optional<InMemoryFinanceStore.RefundData> getRefund(Long refundId, String tenantId, String storeId) {
        InMemoryFinanceStore.RefundData refund = store.refundById().get(refundId);
        if (refund == null) {
            return Optional.empty();
        }
        if (!refund.tenantId().equals(tenantId) || !refund.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(refund);
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
