package com.jianshengfang.ptstudio.core.infrastructure.finance.mysql;

import com.jianshengfang.ptstudio.core.app.finance.FinanceRepository;
import com.jianshengfang.ptstudio.core.app.finance.InMemoryFinanceStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlFinanceRepository implements FinanceRepository {

    private final MysqlFinanceMapper mapper;

    public MysqlFinanceRepository(MysqlFinanceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InMemoryFinanceStore.OrderData createOrder(String tenantId, String storeId, Long memberId,
                                                      String orderType, BigDecimal totalAmount,
                                                      String orderNo, OffsetDateTime createdAt) {
        MysqlFinancePo.OrderPo po = new MysqlFinancePo.OrderPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setOrderNo(orderNo);
        po.setMemberId(memberId);
        po.setOrderType(orderType);
        po.setTotalAmount(totalAmount);
        po.setPaidAmount(BigDecimal.ZERO);
        po.setStatus("CREATED");
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(createdAt);
        mapper.insertOrder(po);
        return toOrder(mapper.getOrder(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public List<InMemoryFinanceStore.OrderData> listOrders(String tenantId, String storeId) {
        return mapper.listOrders(toLong(tenantId), toLong(storeId)).stream().map(this::toOrder).toList();
    }

    @Override
    public Optional<InMemoryFinanceStore.OrderData> getOrder(Long orderId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getOrder(orderId, toLong(tenantId), toLong(storeId))).map(this::toOrder);
    }

    @Override
    public InMemoryFinanceStore.OrderData updateOrderPaid(Long orderId, String tenantId, String storeId,
                                                          BigDecimal paidAmount, String status, OffsetDateTime updatedAt) {
        long t = toLong(tenantId);
        long s = toLong(storeId);
        mapper.updateOrderPaid(orderId, t, s, paidAmount, status, updatedAt);
        return toOrder(mapper.getOrder(orderId, t, s));
    }

    @Override
    public InMemoryFinanceStore.OrderData updateOrderRefunded(Long orderId, String tenantId, String storeId,
                                                              BigDecimal paidAmount, String status, OffsetDateTime updatedAt) {
        return updateOrderPaid(orderId, tenantId, storeId, paidAmount, status, updatedAt);
    }

    @Override
    public InMemoryFinanceStore.PaymentData createPrepayment(Long orderId, String tenantId, String storeId,
                                                             String payNo, String outTradeNo,
                                                             BigDecimal amount, OffsetDateTime createdAt) {
        MysqlFinancePo.PaymentPo po = new MysqlFinancePo.PaymentPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setPayNo(payNo);
        po.setOrderId(orderId);
        po.setPayChannel("ALIPAY");
        po.setOutTradeNo(outTradeNo);
        po.setAmount(amount);
        po.setPayStatus("WAIT_PAY");
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(createdAt);
        mapper.insertPayment(po);
        return toPayment(mapper.getLatestPaymentByOrder(orderId, po.getTenantId(), po.getStoreId()));
    }

    @Override
    public Optional<InMemoryFinanceStore.PaymentData> getPaymentByChannelTradeNo(String tenantId,
                                                                                  String storeId,
                                                                                  String channelTradeNo) {
        return Optional.ofNullable(mapper.getPaymentByChannelTradeNo(toLong(tenantId), toLong(storeId), channelTradeNo))
                .map(this::toPayment);
    }

    @Override
    public Optional<InMemoryFinanceStore.PaymentData> getLatestPaymentByOrder(Long orderId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getLatestPaymentByOrder(orderId, toLong(tenantId), toLong(storeId)))
                .map(this::toPayment);
    }

    @Override
    public InMemoryFinanceStore.PaymentData markPaymentPaid(Long paymentId, String tenantId, String storeId,
                                                            String channelTradeNo, String callbackRaw,
                                                            OffsetDateTime paidAt) {
        long t = toLong(tenantId);
        long s = toLong(storeId);
        mapper.markPaymentPaid(paymentId, t, s, channelTradeNo, callbackRaw, paidAt);
        MysqlFinancePo.PaymentPo po = mapper.getPaymentByChannelTradeNo(t, s, channelTradeNo);
        return toPayment(po);
    }

    @Override
    public Optional<InMemoryFinanceStore.PaymentData> getLatestPaidPaymentByOrder(Long orderId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getLatestPaidPaymentByOrder(orderId, toLong(tenantId), toLong(storeId)))
                .map(this::toPayment);
    }

    @Override
    public InMemoryFinanceStore.RefundData createRefund(Long orderId, Long paymentId,
                                                        String tenantId, String storeId,
                                                        BigDecimal refundAmount, String reason,
                                                        String refundNo, OffsetDateTime createdAt) {
        MysqlFinancePo.RefundPo po = new MysqlFinancePo.RefundPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setRefundNo(refundNo);
        po.setOrderId(orderId);
        po.setPaymentId(paymentId);
        po.setRefundAmount(refundAmount);
        po.setReason(reason);
        po.setStatus("PENDING");
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(createdAt);
        mapper.insertRefund(po);
        return toRefund(mapper.getRefund(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public Optional<InMemoryFinanceStore.RefundData> getRefund(Long refundId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getRefund(refundId, toLong(tenantId), toLong(storeId))).map(this::toRefund);
    }

    @Override
    public InMemoryFinanceStore.RefundData updateRefundDecision(Long refundId, String tenantId, String storeId,
                                                                String status, Long approvedBy, OffsetDateTime approvedAt) {
        long t = toLong(tenantId);
        long s = toLong(storeId);
        mapper.updateRefundDecision(refundId, t, s, status, approvedBy, approvedAt);
        return toRefund(mapper.getRefund(refundId, t, s));
    }

    @Override
    public BigDecimal sumPaidAmountByDate(String tenantId, String storeId, LocalDate bizDate) {
        BigDecimal sum = mapper.sumPaidByDate(toLong(tenantId), toLong(storeId), bizDate);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumApprovedRefundAmountByDate(String tenantId, String storeId, LocalDate bizDate) {
        BigDecimal sum = mapper.sumApprovedRefundByDate(toLong(tenantId), toLong(storeId), bizDate);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private InMemoryFinanceStore.OrderData toOrder(MysqlFinancePo.OrderPo po) {
        return new InMemoryFinanceStore.OrderData(
                po.getId(), po.getOrderNo(), po.getMemberId(),
                String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()), po.getOrderType(),
                po.getTotalAmount(), po.getPaidAmount(), po.getStatus(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private InMemoryFinanceStore.PaymentData toPayment(MysqlFinancePo.PaymentPo po) {
        return new InMemoryFinanceStore.PaymentData(
                po.getId(), po.getOrderId(), String.valueOf(po.getTenantId()), String.valueOf(po.getStoreId()),
                po.getPayChannel(), po.getChannelTradeNo(), po.getAmount(), po.getPayStatus(),
                po.getCallbackRaw(), po.getPaidAt(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private InMemoryFinanceStore.RefundData toRefund(MysqlFinancePo.RefundPo po) {
        return new InMemoryFinanceStore.RefundData(
                po.getId(), po.getRefundNo(), po.getOrderId(), String.valueOf(po.getTenantId()),
                String.valueOf(po.getStoreId()), po.getRefundAmount(), po.getReason(), po.getStatus(),
                po.getApprovedBy(), po.getApprovedAt(), po.getCreatedAt(), po.getUpdatedAt()
        );
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
