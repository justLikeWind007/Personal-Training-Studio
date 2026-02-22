package com.jianshengfang.ptstudio.core.app.schedule;

import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private final ScheduleRepository scheduleRepository;
    private final MemberService memberService;

    public ReservationService(ScheduleRepository scheduleRepository, MemberService memberService) {
        this.scheduleRepository = scheduleRepository;
        this.memberService = memberService;
    }

    public List<InMemoryScheduleStore.SlotData> listAvailableSlots(String tenantId,
                                                                   String storeId,
                                                                   Long coachId,
                                                                   LocalDate slotDate) {
        return scheduleRepository.listAvailableSlots(tenantId, storeId, coachId, slotDate);
    }

    public InMemoryScheduleStore.ReservationData createReservation(CreateReservationCommand command) {
        memberService.get(command.memberId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        InMemoryScheduleStore.SlotData slot = scheduleRepository.getSlot(command.slotId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("时段不存在"));
        if (!slot.status().equals("OPEN")) {
            throw new IllegalArgumentException("该时段不可预约");
        }
        if (slot.bookedCount() >= slot.capacity()) {
            throw new IllegalArgumentException("该时段已约满");
        }

        boolean duplicated = scheduleRepository.existsBookedReservation(
                command.tenantId(), command.storeId(), command.memberId(), command.slotId());
        if (duplicated) {
            throw new IllegalArgumentException("请勿重复预约同一时段");
        }

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.ReservationData reservation = scheduleRepository.createReservation(
                command.tenantId(),
                command.storeId(),
                command.memberId(),
                slot.coachId(),
                slot.id(),
                "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                now
        );
        scheduleRepository.updateSlotBookedCount(
                slot.id(), slot.tenantId(), slot.storeId(), slot.bookedCount() + 1, OffsetDateTime.now());
        return reservation;
    }

    public List<InMemoryScheduleStore.ReservationData> listReservations(String tenantId,
                                                                         String storeId,
                                                                         Long memberId,
                                                                         Long coachId,
                                                                         String status) {
        return scheduleRepository.listReservations(tenantId, storeId, memberId, coachId, status);
    }

    public Optional<InMemoryScheduleStore.ReservationData> getReservation(Long id, String tenantId, String storeId) {
        return scheduleRepository.getReservation(id, tenantId, storeId);
    }

    public InMemoryScheduleStore.ReservationData cancelReservation(Long id,
                                                                   String tenantId,
                                                                   String storeId,
                                                                   String cancelReason) {
        InMemoryScheduleStore.ReservationData reservation = getReservation(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("预约单不存在"));
        if (!reservation.status().equals("BOOKED")) {
            throw new IllegalArgumentException("当前状态不可取消");
        }

        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.ReservationData canceled = scheduleRepository.cancelReservation(
                id,
                tenantId,
                storeId,
                cancelReason,
                now
        );
        InMemoryScheduleStore.SlotData slot = scheduleRepository.getSlot(reservation.slotId(), tenantId, storeId).orElse(null);
        if (slot != null && slot.bookedCount() > 0) {
            scheduleRepository.updateSlotBookedCount(slot.id(), slot.tenantId(), slot.storeId(), slot.bookedCount() - 1, now);
        }
        return canceled;
    }

    public record CreateReservationCommand(String tenantId,
                                           String storeId,
                                           Long memberId,
                                           Long slotId) {
    }
}
