package com.jianshengfang.ptstudio.core.infrastructure.schedule.mysql;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class MysqlSchedulePo {

    public static class CoachPo {
        private Long id;
        private String tenantKey;
        private String storeKey;
        private String coachName;
        private String mobile;
        private String coachLevel;
        private String specialties;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTenantKey() { return tenantKey; }
        public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
        public String getStoreKey() { return storeKey; }
        public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
        public String getCoachName() { return coachName; }
        public void setCoachName(String coachName) { this.coachName = coachName; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
        public String getCoachLevel() { return coachLevel; }
        public void setCoachLevel(String coachLevel) { this.coachLevel = coachLevel; }
        public String getSpecialties() { return specialties; }
        public void setSpecialties(String specialties) { this.specialties = specialties; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class SlotPo {
        private Long id;
        private String tenantKey;
        private String storeKey;
        private Long coachId;
        private LocalDate slotDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer capacity;
        private Integer bookedCount;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTenantKey() { return tenantKey; }
        public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
        public String getStoreKey() { return storeKey; }
        public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
        public Long getCoachId() { return coachId; }
        public void setCoachId(Long coachId) { this.coachId = coachId; }
        public LocalDate getSlotDate() { return slotDate; }
        public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
        public Integer getBookedCount() { return bookedCount; }
        public void setBookedCount(Integer bookedCount) { this.bookedCount = bookedCount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ReservationPo {
        private Long id;
        private String tenantKey;
        private String storeKey;
        private String reservationNo;
        private Long memberId;
        private Long coachId;
        private Long slotId;
        private String status;
        private String cancelReason;
        private OffsetDateTime cancelAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTenantKey() { return tenantKey; }
        public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
        public String getStoreKey() { return storeKey; }
        public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
        public String getReservationNo() { return reservationNo; }
        public void setReservationNo(String reservationNo) { this.reservationNo = reservationNo; }
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public Long getCoachId() { return coachId; }
        public void setCoachId(Long coachId) { this.coachId = coachId; }
        public Long getSlotId() { return slotId; }
        public void setSlotId(Long slotId) { this.slotId = slotId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCancelReason() { return cancelReason; }
        public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
        public OffsetDateTime getCancelAt() { return cancelAt; }
        public void setCancelAt(OffsetDateTime cancelAt) { this.cancelAt = cancelAt; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
