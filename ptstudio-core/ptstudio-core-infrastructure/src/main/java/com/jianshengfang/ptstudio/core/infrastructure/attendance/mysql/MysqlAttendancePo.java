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
}
