package com.jianshengfang.ptstudio.core.infrastructure.attendance.mysql;

import java.time.OffsetDateTime;

public class MysqlAttendancePo {

    public static class CheckinPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String checkinNo;
        private Long reservationId;
        private Long memberId;
        private OffsetDateTime checkinTime;
        private String checkinChannel;
        private Long operatorUserId;
        private String status;
        private OffsetDateTime createdAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getCheckinNo() { return checkinNo; }
        public void setCheckinNo(String checkinNo) { this.checkinNo = checkinNo; }
        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public OffsetDateTime getCheckinTime() { return checkinTime; }
        public void setCheckinTime(OffsetDateTime checkinTime) { this.checkinTime = checkinTime; }
        public String getCheckinChannel() { return checkinChannel; }
        public void setCheckinChannel(String checkinChannel) { this.checkinChannel = checkinChannel; }
        public Long getOperatorUserId() { return operatorUserId; }
        public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class ConsumptionPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String consumptionNo;
        private Long reservationId;
        private Long memberPackageAccountId;
        private String actionType;
        private Integer sessionsDelta;
        private OffsetDateTime consumeTime;
        private Long operatorUserId;
        private String idemKey;
        private String status;
        private OffsetDateTime createdAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getConsumptionNo() { return consumptionNo; }
        public void setConsumptionNo(String consumptionNo) { this.consumptionNo = consumptionNo; }
        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
        public Long getMemberPackageAccountId() { return memberPackageAccountId; }
        public void setMemberPackageAccountId(Long memberPackageAccountId) { this.memberPackageAccountId = memberPackageAccountId; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public Integer getSessionsDelta() { return sessionsDelta; }
        public void setSessionsDelta(Integer sessionsDelta) { this.sessionsDelta = sessionsDelta; }
        public OffsetDateTime getConsumeTime() { return consumeTime; }
        public void setConsumeTime(OffsetDateTime consumeTime) { this.consumeTime = consumeTime; }
        public Long getOperatorUserId() { return operatorUserId; }
        public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
        public String getIdemKey() { return idemKey; }
        public void setIdemKey(String idemKey) { this.idemKey = idemKey; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class ApprovalPo {
        private Long id;
        private Long tenantId;
        private Long storeId;
        private String bizType;
        private Long bizId;
        private String status;
        private String reason;
        private Long submittedBy;
        private OffsetDateTime submittedAt;
        private Long approvedBy;
        private OffsetDateTime approvedAt;
        private String rejectReason;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getBizType() { return bizType; }
        public void setBizType(String bizType) { this.bizType = bizType; }
        public Long getBizId() { return bizId; }
        public void setBizId(Long bizId) { this.bizId = bizId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Long getSubmittedBy() { return submittedBy; }
        public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }
        public OffsetDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
        public Long getApprovedBy() { return approvedBy; }
        public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
        public OffsetDateTime getApprovedAt() { return approvedAt; }
        public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
        public String getRejectReason() { return rejectReason; }
        public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    }
}
