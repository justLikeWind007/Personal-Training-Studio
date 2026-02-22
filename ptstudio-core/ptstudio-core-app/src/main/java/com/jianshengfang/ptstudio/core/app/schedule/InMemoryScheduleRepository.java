package com.jianshengfang.ptstudio.core.app.schedule;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!mysql")
public class InMemoryScheduleRepository implements ScheduleRepository {

    private final InMemoryScheduleStore store;

    public InMemoryScheduleRepository(InMemoryScheduleStore store) {
        this.store = store;
    }

    @Override
    public List<InMemoryScheduleStore.CoachData> listCoaches(String tenantId, String storeId) {
        return store.coachById().values().stream()
                .filter(coach -> coach.tenantId().equals(tenantId) && coach.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryScheduleStore.CoachData::id))
                .toList();
    }

    @Override
    public InMemoryScheduleStore.CoachData createCoach(String tenantId, String storeId, String name,
                                                       String mobile, String level, String specialties) {
        long id = store.nextCoachId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.CoachData coach = new InMemoryScheduleStore.CoachData(
                id, tenantId, storeId, name, mobile, level, specialties, "ACTIVE", now, now
        );
        store.coachById().put(id, coach);
        return coach;
    }

    @Override
    public Optional<InMemoryScheduleStore.CoachData> getCoach(Long coachId, String tenantId, String storeId) {
        InMemoryScheduleStore.CoachData coach = store.coachById().get(coachId);
        if (coach == null) {
            return Optional.empty();
        }
        if (!coach.tenantId().equals(tenantId) || !coach.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(coach);
    }

    @Override
    public List<InMemoryScheduleStore.SlotData> listCoachSlots(Long coachId, String tenantId, String storeId) {
        return store.listSlotsByCoach(coachId).stream()
                .filter(slot -> slot.tenantId().equals(tenantId) && slot.storeId().equals(storeId))
                .toList();
    }

    @Override
    public InMemoryScheduleStore.SlotData createSlot(Long coachId, String tenantId, String storeId,
                                                     LocalDate slotDate, LocalTime startTime,
                                                     LocalTime endTime, int capacity) {
        long id = store.nextSlotId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.SlotData slot = new InMemoryScheduleStore.SlotData(
                id, coachId, tenantId, storeId, slotDate, startTime, endTime,
                capacity, 0, "OPEN", now, now
        );
        store.slotById().put(id, slot);
        return slot;
    }

    @Override
    public Optional<InMemoryScheduleStore.SlotData> getSlot(Long slotId, String tenantId, String storeId) {
        InMemoryScheduleStore.SlotData slot = store.slotById().get(slotId);
        if (slot == null) {
            return Optional.empty();
        }
        if (!slot.tenantId().equals(tenantId) || !slot.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(slot);
    }

    @Override
    public InMemoryScheduleStore.SlotData updateSlotBookedCount(Long slotId, String tenantId, String storeId,
                                                                int bookedCount, OffsetDateTime updatedAt) {
        InMemoryScheduleStore.SlotData existing = getSlot(slotId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("时段不存在"));
        InMemoryScheduleStore.SlotData updated = new InMemoryScheduleStore.SlotData(
                existing.id(), existing.coachId(), existing.tenantId(), existing.storeId(),
                existing.slotDate(), existing.startTime(), existing.endTime(), existing.capacity(),
                bookedCount, existing.status(), existing.createdAt(), updatedAt
        );
        store.slotById().put(slotId, updated);
        return updated;
    }

    @Override
    public List<InMemoryScheduleStore.SlotData> listAvailableSlots(String tenantId, String storeId,
                                                                   Long coachId, LocalDate slotDate) {
        return store.slotById().values().stream()
                .filter(slot -> slot.tenantId().equals(tenantId) && slot.storeId().equals(storeId))
                .filter(slot -> slot.status().equals("OPEN"))
                .filter(slot -> slot.bookedCount() < slot.capacity())
                .filter(slot -> coachId == null || slot.coachId().equals(coachId))
                .filter(slot -> slotDate == null || slot.slotDate().equals(slotDate))
                .sorted(Comparator.comparing(InMemoryScheduleStore.SlotData::slotDate)
                        .thenComparing(InMemoryScheduleStore.SlotData::startTime))
                .toList();
    }

    @Override
    public boolean existsBookedReservation(String tenantId, String storeId, Long memberId, Long slotId) {
        return store.reservationById().values().stream()
                .anyMatch(r -> r.tenantId().equals(tenantId)
                        && r.storeId().equals(storeId)
                        && r.memberId().equals(memberId)
                        && r.slotId().equals(slotId)
                        && r.status().equals("BOOKED"));
    }

    @Override
    public InMemoryScheduleStore.ReservationData createReservation(String tenantId, String storeId, Long memberId,
                                                                   Long coachId, Long slotId,
                                                                   String reservationNo, OffsetDateTime createdAt) {
        long id = store.nextReservationId();
        InMemoryScheduleStore.ReservationData reservation = new InMemoryScheduleStore.ReservationData(
                id, reservationNo, memberId, coachId, slotId, tenantId, storeId,
                "BOOKED", null, null, createdAt, createdAt
        );
        store.reservationById().put(id, reservation);
        return reservation;
    }

    @Override
    public List<InMemoryScheduleStore.ReservationData> listReservations(String tenantId, String storeId,
                                                                        Long memberId, Long coachId, String status) {
        return store.reservationById().values().stream()
                .filter(r -> r.tenantId().equals(tenantId) && r.storeId().equals(storeId))
                .filter(r -> memberId == null || r.memberId().equals(memberId))
                .filter(r -> coachId == null || r.coachId().equals(coachId))
                .filter(r -> status == null || r.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(InMemoryScheduleStore.ReservationData::id))
                .toList();
    }

    @Override
    public Optional<InMemoryScheduleStore.ReservationData> getReservation(Long id, String tenantId, String storeId) {
        InMemoryScheduleStore.ReservationData reservation = store.reservationById().get(id);
        if (reservation == null) {
            return Optional.empty();
        }
        if (!reservation.tenantId().equals(tenantId) || !reservation.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(reservation);
    }

    @Override
    public InMemoryScheduleStore.ReservationData cancelReservation(Long id, String tenantId, String storeId,
                                                                   String cancelReason, OffsetDateTime cancelAt) {
        InMemoryScheduleStore.ReservationData existing = getReservation(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("预约单不存在"));
        InMemoryScheduleStore.ReservationData canceled = new InMemoryScheduleStore.ReservationData(
                existing.id(), existing.reservationNo(), existing.memberId(), existing.coachId(), existing.slotId(),
                existing.tenantId(), existing.storeId(), "CANCELED", cancelReason, cancelAt,
                existing.createdAt(), cancelAt
        );
        store.reservationById().put(id, canceled);
        return canceled;
    }
}
