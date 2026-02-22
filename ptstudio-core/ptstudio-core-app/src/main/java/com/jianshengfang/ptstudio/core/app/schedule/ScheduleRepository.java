package com.jianshengfang.ptstudio.core.app.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {

    List<InMemoryScheduleStore.CoachData> listCoaches(String tenantId, String storeId);

    InMemoryScheduleStore.CoachData createCoach(String tenantId,
                                                String storeId,
                                                String name,
                                                String mobile,
                                                String level,
                                                String specialties);

    Optional<InMemoryScheduleStore.CoachData> getCoach(Long coachId, String tenantId, String storeId);

    List<InMemoryScheduleStore.SlotData> listCoachSlots(Long coachId, String tenantId, String storeId);

    InMemoryScheduleStore.SlotData createSlot(Long coachId,
                                              String tenantId,
                                              String storeId,
                                              LocalDate slotDate,
                                              LocalTime startTime,
                                              LocalTime endTime,
                                              int capacity);

    Optional<InMemoryScheduleStore.SlotData> getSlot(Long slotId, String tenantId, String storeId);

    InMemoryScheduleStore.SlotData updateSlotBookedCount(Long slotId,
                                                         String tenantId,
                                                         String storeId,
                                                         int bookedCount,
                                                         OffsetDateTime updatedAt);

    List<InMemoryScheduleStore.SlotData> listAvailableSlots(String tenantId,
                                                            String storeId,
                                                            Long coachId,
                                                            LocalDate slotDate);

    boolean existsBookedReservation(String tenantId, String storeId, Long memberId, Long slotId);

    InMemoryScheduleStore.ReservationData createReservation(String tenantId,
                                                            String storeId,
                                                            Long memberId,
                                                            Long coachId,
                                                            Long slotId,
                                                            String reservationNo,
                                                            OffsetDateTime createdAt);

    List<InMemoryScheduleStore.ReservationData> listReservations(String tenantId,
                                                                 String storeId,
                                                                 Long memberId,
                                                                 Long coachId,
                                                                 String status);

    Optional<InMemoryScheduleStore.ReservationData> getReservation(Long id, String tenantId, String storeId);

    InMemoryScheduleStore.ReservationData cancelReservation(Long id,
                                                            String tenantId,
                                                            String storeId,
                                                            String cancelReason,
                                                            OffsetDateTime cancelAt);
}
