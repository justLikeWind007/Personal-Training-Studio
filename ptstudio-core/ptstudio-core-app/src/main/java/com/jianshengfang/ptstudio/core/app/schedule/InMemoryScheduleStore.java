package com.jianshengfang.ptstudio.core.app.schedule;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryScheduleStore {

    private final AtomicLong coachIdGenerator = new AtomicLong(1);
    private final AtomicLong slotIdGenerator = new AtomicLong(1);
    private final AtomicLong reservationIdGenerator = new AtomicLong(1);

    private final Map<Long, CoachData> coachById = new ConcurrentHashMap<>();
    private final Map<Long, SlotData> slotById = new ConcurrentHashMap<>();
    private final Map<Long, ReservationData> reservationById = new ConcurrentHashMap<>();

    public long nextCoachId() {
        return coachIdGenerator.getAndIncrement();
    }

    public long nextSlotId() {
        return slotIdGenerator.getAndIncrement();
    }

    public long nextReservationId() {
        return reservationIdGenerator.getAndIncrement();
    }

    public Map<Long, CoachData> coachById() {
        return coachById;
    }

    public Map<Long, SlotData> slotById() {
        return slotById;
    }

    public Map<Long, ReservationData> reservationById() {
        return reservationById;
    }

    public record CoachData(Long id,
                            String tenantId,
                            String storeId,
                            String name,
                            String mobile,
                            String level,
                            String specialties,
                            String status,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {
    }

    public record SlotData(Long id,
                           Long coachId,
                           String tenantId,
                           String storeId,
                           LocalDate slotDate,
                           LocalTime startTime,
                           LocalTime endTime,
                           int capacity,
                           int bookedCount,
                           String status,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {
    }

    public record ReservationData(Long id,
                                  String reservationNo,
                                  Long memberId,
                                  Long coachId,
                                  Long slotId,
                                  String tenantId,
                                  String storeId,
                                  String status,
                                  String cancelReason,
                                  OffsetDateTime cancelAt,
                                  OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt) {
    }

    public List<SlotData> listSlotsByCoach(Long coachId) {
        return slotById.values().stream()
                .filter(slot -> slot.coachId().equals(coachId))
                .sorted((a, b) -> {
                    int dateCompare = a.slotDate().compareTo(b.slotDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return a.startTime().compareTo(b.startTime());
                })
                .toList();
    }
}
