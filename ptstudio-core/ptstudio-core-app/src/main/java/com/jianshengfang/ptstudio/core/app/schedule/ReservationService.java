package com.jianshengfang.ptstudio.core.app.schedule;

import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private final InMemoryScheduleStore scheduleStore;
    private final MemberService memberService;

    public ReservationService(InMemoryScheduleStore scheduleStore, MemberService memberService) {
        this.scheduleStore = scheduleStore;
        this.memberService = memberService;
    }

    public List<InMemoryScheduleStore.SlotData> listAvailableSlots(String tenantId,
                                                                   String storeId,
                                                                   Long coachId,
                                                                   LocalDate slotDate) {
        return scheduleStore.slotById().values().stream()
                .filter(slot -> slot.tenantId().equals(tenantId) && slot.storeId().equals(storeId))
                .filter(slot -> slot.status().equals("OPEN"))
                .filter(slot -> slot.bookedCount() < slot.capacity())
                .filter(slot -> coachId == null || slot.coachId().equals(coachId))
                .filter(slot -> slotDate == null || slot.slotDate().equals(slotDate))
                .sorted(Comparator.comparing(InMemoryScheduleStore.SlotData::slotDate)
                        .thenComparing(InMemoryScheduleStore.SlotData::startTime))
                .toList();
    }

    public InMemoryScheduleStore.ReservationData createReservation(CreateReservationCommand command) {
        memberService.get(command.memberId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        InMemoryScheduleStore.SlotData slot = Optional.ofNullable(scheduleStore.slotById().get(command.slotId()))
                .orElseThrow(() -> new IllegalArgumentException("时段不存在"));
        if (!slot.tenantId().equals(command.tenantId()) || !slot.storeId().equals(command.storeId())) {
            throw new IllegalArgumentException("时段不存在");
        }
        if (!slot.status().equals("OPEN")) {
            throw new IllegalArgumentException("该时段不可预约");
        }
        if (slot.bookedCount() >= slot.capacity()) {
            throw new IllegalArgumentException("该时段已约满");
        }

        boolean duplicated = scheduleStore.reservationById().values().stream()
                .anyMatch(r -> r.tenantId().equals(command.tenantId())
                        && r.storeId().equals(command.storeId())
                        && r.memberId().equals(command.memberId())
                        && r.slotId().equals(command.slotId())
                        && r.status().equals("BOOKED"));
        if (duplicated) {
            throw new IllegalArgumentException("请勿重复预约同一时段");
        }

        long id = scheduleStore.nextReservationId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.ReservationData reservation = new InMemoryScheduleStore.ReservationData(
                id,
                String.format("R%08d", id),
                command.memberId(),
                slot.coachId(),
                slot.id(),
                command.tenantId(),
                command.storeId(),
                "BOOKED",
                null,
                null,
                now,
                now
        );
        scheduleStore.reservationById().put(id, reservation);

        InMemoryScheduleStore.SlotData updatedSlot = new InMemoryScheduleStore.SlotData(
                slot.id(),
                slot.coachId(),
                slot.tenantId(),
                slot.storeId(),
                slot.slotDate(),
                slot.startTime(),
                slot.endTime(),
                slot.capacity(),
                slot.bookedCount() + 1,
                slot.status(),
                slot.createdAt(),
                OffsetDateTime.now()
        );
        scheduleStore.slotById().put(slot.id(), updatedSlot);
        return reservation;
    }

    public List<InMemoryScheduleStore.ReservationData> listReservations(String tenantId,
                                                                         String storeId,
                                                                         Long memberId,
                                                                         Long coachId,
                                                                         String status) {
        return scheduleStore.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> memberId == null || r.memberId().equals(memberId))
                .filter(r -> coachId == null || r.coachId().equals(coachId))
                .filter(r -> status == null || r.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(InMemoryScheduleStore.ReservationData::id))
                .toList();
    }

    public Optional<InMemoryScheduleStore.ReservationData> getReservation(Long id, String tenantId, String storeId) {
        InMemoryScheduleStore.ReservationData reservation = scheduleStore.reservationById().get(id);
        if (reservation == null) {
            return Optional.empty();
        }
        if (!reservation.tenantId().equals(tenantId) || !reservation.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(reservation);
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

        InMemoryScheduleStore.ReservationData canceled = new InMemoryScheduleStore.ReservationData(
                reservation.id(),
                reservation.reservationNo(),
                reservation.memberId(),
                reservation.coachId(),
                reservation.slotId(),
                reservation.tenantId(),
                reservation.storeId(),
                "CANCELED",
                cancelReason,
                OffsetDateTime.now(),
                reservation.createdAt(),
                OffsetDateTime.now()
        );
        scheduleStore.reservationById().put(id, canceled);

        InMemoryScheduleStore.SlotData slot = scheduleStore.slotById().get(reservation.slotId());
        if (slot != null && slot.bookedCount() > 0) {
            InMemoryScheduleStore.SlotData slotRollback = new InMemoryScheduleStore.SlotData(
                    slot.id(),
                    slot.coachId(),
                    slot.tenantId(),
                    slot.storeId(),
                    slot.slotDate(),
                    slot.startTime(),
                    slot.endTime(),
                    slot.capacity(),
                    slot.bookedCount() - 1,
                    slot.status(),
                    slot.createdAt(),
                    OffsetDateTime.now()
            );
            scheduleStore.slotById().put(slot.id(), slotRollback);
        }
        return canceled;
    }

    public record CreateReservationCommand(String tenantId,
                                           String storeId,
                                           Long memberId,
                                           Long slotId) {
    }
}
