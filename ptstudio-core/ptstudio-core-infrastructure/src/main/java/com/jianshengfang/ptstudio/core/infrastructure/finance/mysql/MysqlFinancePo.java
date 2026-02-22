package com.jianshengfang.ptstudio.core.infrastructure.finance.mysql;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MysqlFinancePo {

    public static class OrderPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String orderNo;
        private Long memberId;
        private String orderType;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class PaymentPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String payNo;
        private Long orderId;
        private String payChannel;
        private String outTradeNo;
        private String channelTradeNo;
        private BigDecimal amount;
        private String payStatus;
        private String callbackRaw;
        private OffsetDateTime paidAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getPayNo() { return payNo; }
        public void setPayNo(String payNo) { this.payNo = payNo; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getPayChannel() { return payChannel; }
        public void setPayChannel(String payChannel) { this.payChannel = payChannel; }
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
        public String getChannelTradeNo() { return channelTradeNo; }
        public void setChannelTradeNo(String channelTradeNo) { this.channelTradeNo = channelTradeNo; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPayStatus() { return payStatus; }
        public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
        public String getCallbackRaw() { return callbackRaw; }
        public void setCallbackRaw(String callbackRaw) { this.callbackRaw = callbackRaw; }
        public OffsetDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class RefundPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String refundNo;
        private Long orderId;
        private Long paymentId;
        private BigDecimal refundAmount;
        private String reason;
        private String status;
        private Long approvedBy;
        private OffsetDateTime approvedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getRefundNo() { return refundNo; }
        public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
        public BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getApprovedBy() { return approvedBy; }
        public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
        public OffsetDateTime getApprovedAt() { return approvedAt; }
        public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
