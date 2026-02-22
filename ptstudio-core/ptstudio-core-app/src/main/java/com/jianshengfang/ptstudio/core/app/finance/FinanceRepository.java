package com.jianshengfang.ptstudio.core.app.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FinanceRepository {

    InMemoryFinanceStore.OrderData createOrder(String tenantId,
                                               String storeId,
                                               Long memberId,
                                               String orderType,
                                               BigDecimal totalAmount,
                                               String orderNo,
                                               OffsetDateTime createdAt);

    List<InMemoryFinanceStore.OrderData> listOrders(String tenantId, String storeId);

    Optional<InMemoryFinanceStore.OrderData> getOrder(Long orderId, String tenantId, String storeId);

    InMemoryFinanceStore.OrderData updateOrderPaid(Long orderId,
                                                   String tenantId,
                                                   String storeId,
                                                   BigDecimal paidAmount,
                                                   String status,
                                                   OffsetDateTime updatedAt);

    InMemoryFinanceStore.OrderData updateOrderRefunded(Long orderId,
                                                       String tenantId,
                                                       String storeId,
                                                       BigDecimal paidAmount,
                                                       String status,
                                                       OffsetDateTime updatedAt);

    InMemoryFinanceStore.PaymentData createPrepayment(Long orderId,
                                                      String tenantId,
                                                      String storeId,
                                                      String payNo,
                                                      String outTradeNo,
                                                      BigDecimal amount,
                                                      OffsetDateTime createdAt);

    List<InMemoryFinanceStore.PaymentData> listPayments(String tenantId, String storeId);

    Optional<InMemoryFinanceStore.PaymentData> getPaymentByChannelTradeNo(String tenantId,
                                                                          String storeId,
                                                                          String channelTradeNo);

    Optional<InMemoryFinanceStore.PaymentData> getLatestPaymentByOrder(Long orderId, String tenantId, String storeId);

    InMemoryFinanceStore.PaymentData markPaymentPaid(Long paymentId,
                                                     String tenantId,
                                                     String storeId,
                                                     String channelTradeNo,
                                                     String callbackRaw,
                                                     OffsetDateTime paidAt);

    Optional<InMemoryFinanceStore.PaymentData> getLatestPaidPaymentByOrder(Long orderId, String tenantId, String storeId);

    InMemoryFinanceStore.RefundData createRefund(Long orderId,
                                                 Long paymentId,
                                                 String tenantId,
                                                 String storeId,
                                                 BigDecimal refundAmount,
                                                 String reason,
                                                 String refundNo,
                                                 OffsetDateTime createdAt);

    Optional<InMemoryFinanceStore.RefundData> getRefund(Long refundId, String tenantId, String storeId);

    List<InMemoryFinanceStore.RefundData> listRefunds(String tenantId, String storeId);

    InMemoryFinanceStore.RefundData updateRefundDecision(Long refundId,
                                                         String tenantId,
                                                         String storeId,
                                                         String status,
                                                         Long approvedBy,
                                                         OffsetDateTime approvedAt);

    BigDecimal sumReservedRefundAmountByOrder(Long orderId, String tenantId, String storeId);

    BigDecimal sumApprovedRefundAmountByOrder(Long orderId, String tenantId, String storeId);

    BigDecimal sumPaidAmountByDate(String tenantId, String storeId, LocalDate bizDate);

    BigDecimal sumApprovedRefundAmountByDate(String tenantId, String storeId, LocalDate bizDate);
}
