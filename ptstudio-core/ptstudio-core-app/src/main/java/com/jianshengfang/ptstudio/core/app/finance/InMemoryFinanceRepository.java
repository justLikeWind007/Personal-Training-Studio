package com.jianshengfang.ptstudio.core.app.finance;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!mysql")
public class InMemoryFinanceRepository implements FinanceRepository {

    private final InMemoryFinanceStore store;

    public InMemoryFinanceRepository(InMemoryFinanceStore store) {
        this.store = store;
    }

    @Override
    public InMemoryFinanceStore.OrderData createOrder(String tenantId, String storeId, Long memberId,
                                                      String orderType, BigDecimal totalAmount,
                                                      String orderNo, OffsetDateTime createdAt) {
        long id = store.nextOrderId();
        InMemoryFinanceStore.OrderData order = new InMemoryFinanceStore.OrderData(
                id, orderNo, memberId, tenantId, storeId, orderType, totalAmount,
                BigDecimal.ZERO, "CREATED", createdAt, createdAt
        );
        store.orderById().put(id, order);
        return order;
    }

    @Override
    public List<InMemoryFinanceStore.OrderData> listOrders(String tenantId, String storeId) {
        return store.orderById().values().stream()
                .filter(order -> order.tenantId().equals(tenantId) && order.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryFinanceStore.OrderData::id))
                .toList();
    }

    @Override
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

    @Override
    public InMemoryFinanceStore.OrderData updateOrderPaid(Long orderId, String tenantId, String storeId,
                                                          BigDecimal paidAmount, String status, OffsetDateTime updatedAt) {
        InMemoryFinanceStore.OrderData order = getOrder(orderId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        InMemoryFinanceStore.OrderData updated = new InMemoryFinanceStore.OrderData(
                order.id(), order.orderNo(), order.memberId(), order.tenantId(), order.storeId(),
                order.orderType(), order.totalAmount(), paidAmount, status, order.createdAt(), updatedAt
        );
        store.orderById().put(orderId, updated);
        return updated;
    }

    @Override
    public InMemoryFinanceStore.OrderData updateOrderRefunded(Long orderId, String tenantId, String storeId,
                                                              BigDecimal paidAmount, String status, OffsetDateTime updatedAt) {
        return updateOrderPaid(orderId, tenantId, storeId, paidAmount, status, updatedAt);
    }

    @Override
    public InMemoryFinanceStore.PaymentData createPrepayment(Long orderId, String tenantId, String storeId,
                                                             String payNo, String outTradeNo, BigDecimal amount,
                                                             OffsetDateTime createdAt) {
        long paymentId = store.nextPaymentId();
        InMemoryFinanceStore.PaymentData payment = new InMemoryFinanceStore.PaymentData(
                paymentId, orderId, tenantId, storeId, "ALIPAY", null, amount,
                "WAIT_PAY", null, null, createdAt, createdAt
        );
        store.paymentById().put(paymentId, payment);
        return payment;
    }

    @Override
    public Optional<InMemoryFinanceStore.PaymentData> getPaymentByChannelTradeNo(String tenantId,
                                                                                  String storeId,
                                                                                  String channelTradeNo) {
        Long id = store.paymentIdByTradeNo().get(channelTradeNo);
        if (id == null) {
            return Optional.empty();
        }
        InMemoryFinanceStore.PaymentData payment = store.paymentById().get(id);
        if (payment == null) {
            return Optional.empty();
        }
        if (!payment.tenantId().equals(tenantId) || !payment.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(payment);
    }

    @Override
    public Optional<InMemoryFinanceStore.PaymentData> getLatestPaymentByOrder(Long orderId, String tenantId, String storeId) {
        return store.paymentById().values().stream()
                .filter(p -> p.orderId().equals(orderId)
                        && p.tenantId().equals(tenantId)
                        && p.storeId().equals(storeId))
                .max(Comparator.comparing(InMemoryFinanceStore.PaymentData::id));
    }

    @Override
    public InMemoryFinanceStore.PaymentData markPaymentPaid(Long paymentId, String tenantId, String storeId,
                                                            String channelTradeNo, String callbackRaw,
                                                            OffsetDateTime paidAt) {
        InMemoryFinanceStore.PaymentData payment = store.paymentById().get(paymentId);
        if (payment == null || !payment.tenantId().equals(tenantId) || !payment.storeId().equals(storeId)) {
            throw new IllegalArgumentException("支付单不存在");
        }
        InMemoryFinanceStore.PaymentData paid = new InMemoryFinanceStore.PaymentData(
                payment.id(), payment.orderId(), payment.tenantId(), payment.storeId(), payment.payChannel(),
                channelTradeNo, payment.amount(), "PAID", callbackRaw, paidAt, payment.createdAt(), paidAt
        );
        store.paymentById().put(paymentId, paid);
        store.paymentIdByTradeNo().put(channelTradeNo, paymentId);
        return paid;
    }

    @Override
    public Optional<InMemoryFinanceStore.PaymentData> getLatestPaidPaymentByOrder(Long orderId, String tenantId, String storeId) {
        return store.paymentById().values().stream()
                .filter(p -> p.orderId().equals(orderId)
                        && p.tenantId().equals(tenantId)
                        && p.storeId().equals(storeId)
                        && "PAID".equals(p.payStatus()))
                .max(Comparator.comparing(InMemoryFinanceStore.PaymentData::id));
    }

    @Override
    public InMemoryFinanceStore.RefundData createRefund(Long orderId, Long paymentId,
                                                        String tenantId, String storeId,
                                                        BigDecimal refundAmount, String reason,
                                                        String refundNo, OffsetDateTime createdAt) {
        long id = store.nextRefundId();
        InMemoryFinanceStore.RefundData refund = new InMemoryFinanceStore.RefundData(
                id, refundNo, orderId, tenantId, storeId, refundAmount, reason,
                "PENDING", null, null, createdAt, createdAt
        );
        store.refundById().put(id, refund);
        return refund;
    }

    @Override
    public Optional<InMemoryFinanceStore.RefundData> getRefund(Long refundId, String tenantId, String storeId) {
        InMemoryFinanceStore.RefundData refund = store.refundById().get(refundId);
        if (refund == null) {
            return Optional.empty();
        }
        if (!refund.tenantId().equals(tenantId) || !refund.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(refund);
    }

    @Override
    public InMemoryFinanceStore.RefundData updateRefundDecision(Long refundId, String tenantId, String storeId,
                                                                String status, Long approvedBy, OffsetDateTime approvedAt) {
        InMemoryFinanceStore.RefundData refund = getRefund(refundId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        InMemoryFinanceStore.RefundData updated = new InMemoryFinanceStore.RefundData(
                refund.id(), refund.refundNo(), refund.orderId(), refund.tenantId(), refund.storeId(),
                refund.refundAmount(), refund.reason(), status, approvedBy, approvedAt,
                refund.createdAt(), approvedAt
        );
        store.refundById().put(refundId, updated);
        return updated;
    }

    @Override
    public BigDecimal sumPaidAmountByDate(String tenantId, String storeId, LocalDate bizDate) {
        return store.paymentById().values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.storeId().equals(storeId))
                .filter(p -> "PAID".equals(p.payStatus()))
                .filter(p -> p.paidAt() != null && p.paidAt().toLocalDate().equals(bizDate))
                .map(InMemoryFinanceStore.PaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumApprovedRefundAmountByDate(String tenantId, String storeId, LocalDate bizDate) {
        return store.refundById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> "APPROVED".equals(r.status()))
                .filter(r -> r.approvedAt() != null && r.approvedAt().toLocalDate().equals(bizDate))
                .map(InMemoryFinanceStore.RefundData::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
