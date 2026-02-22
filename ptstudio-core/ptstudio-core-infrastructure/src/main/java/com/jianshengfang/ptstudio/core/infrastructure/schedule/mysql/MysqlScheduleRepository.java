package com.jianshengfang.ptstudio.core.infrastructure.schedule.mysql;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import com.jianshengfang.ptstudio.core.app.schedule.ScheduleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlScheduleRepository implements ScheduleRepository {

    private final MysqlScheduleMapper mapper;

    public MysqlScheduleRepository(MysqlScheduleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<InMemoryScheduleStore.CoachData> listCoaches(String tenantId, String storeId) {
        return mapper.listCoaches(tenantId, storeId).stream().map(this::toCoachData).toList();
    }

    @Override
    public InMemoryScheduleStore.CoachData createCoach(String tenantId, String storeId, String name,
                                                       String mobile, String level, String specialties) {
        MysqlSchedulePo.CoachPo po = new MysqlSchedulePo.CoachPo();
        po.setTenantKey(tenantId);
        po.setStoreKey(storeId);
        po.setCoachName(name);
        po.setMobile(mobile);
        po.setCoachLevel(level);
        po.setSpecialties(specialties);
        po.setStatus("ACTIVE");
        mapper.insertCoach(po);
        return toCoachData(mapper.getCoach(po.getId(), tenantId, storeId));
    }

    @Override
    public Optional<InMemoryScheduleStore.CoachData> getCoach(Long coachId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getCoach(coachId, tenantId, storeId)).map(this::toCoachData);
    }

    @Override
    public List<InMemoryScheduleStore.SlotData> listCoachSlots(Long coachId, String tenantId, String storeId) {
        return mapper.listCoachSlots(coachId, tenantId, storeId).stream().map(this::toSlotData).toList();
    }

    @Override
    public InMemoryScheduleStore.SlotData createSlot(Long coachId, String tenantId, String storeId,
                                                     LocalDate slotDate, LocalTime startTime,
                                                     LocalTime endTime, int capacity) {
        MysqlSchedulePo.SlotPo po = new MysqlSchedulePo.SlotPo();
        po.setTenantKey(tenantId);
        po.setStoreKey(storeId);
        po.setCoachId(coachId);
        po.setSlotDate(slotDate);
        po.setStartTime(startTime);
        po.setEndTime(endTime);
        po.setCapacity(capacity);
        po.setBookedCount(0);
        po.setStatus("OPEN");
        mapper.insertSlot(po);
        return toSlotData(mapper.getSlot(po.getId(), tenantId, storeId));
    }

    @Override
    public Optional<InMemoryScheduleStore.SlotData> getSlot(Long slotId, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getSlot(slotId, tenantId, storeId)).map(this::toSlotData);
    }

    @Override
    public InMemoryScheduleStore.SlotData updateSlotBookedCount(Long slotId, String tenantId, String storeId,
                                                                int bookedCount, OffsetDateTime updatedAt) {
        mapper.updateSlotBookedCount(slotId, tenantId, storeId, bookedCount, updatedAt);
        return toSlotData(mapper.getSlot(slotId, tenantId, storeId));
    }

    @Override
    public List<InMemoryScheduleStore.SlotData> listAvailableSlots(String tenantId, String storeId,
                                                                   Long coachId, LocalDate slotDate) {
        return mapper.listAvailableSlots(tenantId, storeId, coachId, slotDate).stream().map(this::toSlotData).toList();
    }

    @Override
    public boolean existsBookedReservation(String tenantId, String storeId, Long memberId, Long slotId) {
        return mapper.countBookedReservation(tenantId, storeId, memberId, slotId) > 0;
    }

    @Override
    public InMemoryScheduleStore.ReservationData createReservation(String tenantId, String storeId,
                                                                   Long memberId, Long coachId, Long slotId,
                                                                   String reservationNo, OffsetDateTime createdAt) {
        MysqlSchedulePo.ReservationPo po = new MysqlSchedulePo.ReservationPo();
        po.setTenantKey(tenantId);
        po.setStoreKey(storeId);
        po.setReservationNo(reservationNo);
        po.setMemberId(memberId);
        po.setCoachId(coachId);
        po.setSlotId(slotId);
        po.setStatus("BOOKED");
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(createdAt);
        mapper.insertReservation(po);
        return toReservationData(mapper.getReservation(po.getId(), tenantId, storeId));
    }

    @Override
    public List<InMemoryScheduleStore.ReservationData> listReservations(String tenantId, String storeId,
                                                                        Long memberId, Long coachId, String status) {
        return mapper.listReservations(tenantId, storeId, memberId, coachId, status).stream()
                .map(this::toReservationData).toList();
    }

    @Override
    public Optional<InMemoryScheduleStore.ReservationData> getReservation(Long id, String tenantId, String storeId) {
        return Optional.ofNullable(mapper.getReservation(id, tenantId, storeId)).map(this::toReservationData);
    }

    @Override
    public InMemoryScheduleStore.ReservationData cancelReservation(Long id, String tenantId, String storeId,
                                                                   String cancelReason, OffsetDateTime cancelAt) {
        mapper.cancelReservation(id, tenantId, storeId, cancelReason, cancelAt);
        return toReservationData(mapper.getReservation(id, tenantId, storeId));
    }

    private InMemoryScheduleStore.CoachData toCoachData(MysqlSchedulePo.CoachPo po) {
        return new InMemoryScheduleStore.CoachData(
                po.getId(), po.getTenantKey(), po.getStoreKey(), po.getCoachName(), po.getMobile(),
                po.getCoachLevel(), po.getSpecialties(), po.getStatus(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private InMemoryScheduleStore.SlotData toSlotData(MysqlSchedulePo.SlotPo po) {
        return new InMemoryScheduleStore.SlotData(
                po.getId(), po.getCoachId(), po.getTenantKey(), po.getStoreKey(), po.getSlotDate(),
                po.getStartTime(), po.getEndTime(), po.getCapacity(), po.getBookedCount(),
                po.getStatus(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private InMemoryScheduleStore.ReservationData toReservationData(MysqlSchedulePo.ReservationPo po) {
        return new InMemoryScheduleStore.ReservationData(
                po.getId(), po.getReservationNo(), po.getMemberId(), po.getCoachId(), po.getSlotId(),
                po.getTenantKey(), po.getStoreKey(), po.getStatus(), po.getCancelReason(),
                po.getCancelAt(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }
}
