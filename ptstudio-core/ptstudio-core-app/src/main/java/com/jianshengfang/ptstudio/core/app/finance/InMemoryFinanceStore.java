package com.jianshengfang.ptstudio.core.app.finance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryFinanceStore {

    private final AtomicLong orderIdGenerator = new AtomicLong(1);
    private final AtomicLong paymentIdGenerator = new AtomicLong(1);
    private final AtomicLong refundIdGenerator = new AtomicLong(1);

    private final Map<Long, OrderData> orderById = new ConcurrentHashMap<>();
    private final Map<Long, PaymentData> paymentById = new ConcurrentHashMap<>();
    private final Map<Long, RefundData> refundById = new ConcurrentHashMap<>();
    private final Map<String, Long> paymentIdByTradeNo = new ConcurrentHashMap<>();

    public long nextOrderId() {
        return orderIdGenerator.getAndIncrement();
    }

    public long nextPaymentId() {
        return paymentIdGenerator.getAndIncrement();
    }

    public long nextRefundId() {
        return refundIdGenerator.getAndIncrement();
    }

    public Map<Long, OrderData> orderById() {
        return orderById;
    }

    public Map<Long, PaymentData> paymentById() {
        return paymentById;
    }

    public Map<Long, RefundData> refundById() {
        return refundById;
    }

    public Map<String, Long> paymentIdByTradeNo() {
        return paymentIdByTradeNo;
    }

    public record OrderData(Long id,
                            String orderNo,
                            Long memberId,
                            String tenantId,
                            String storeId,
                            String orderType,
                            BigDecimal totalAmount,
                            BigDecimal paidAmount,
                            String status,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {
    }

    public record PaymentData(Long id,
                              Long orderId,
                              String tenantId,
                              String storeId,
                              String payChannel,
                              String channelTradeNo,
                              BigDecimal amount,
                              String payStatus,
                              String callbackRaw,
                              OffsetDateTime paidAt,
                              OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
    }

    public record RefundData(Long id,
                             String refundNo,
                             Long orderId,
                             String tenantId,
                             String storeId,
                             BigDecimal refundAmount,
                             String reason,
                             String status,
                             Long approvedBy,
                             OffsetDateTime approvedAt,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
    }

    public record DailyReconcileData(LocalDate bizDate,
                                     String tenantId,
                                     String storeId,
                                     BigDecimal expectedAmount,
                                     BigDecimal actualAmount,
                                     BigDecimal diffAmount,
                                     String status) {
    }
}
