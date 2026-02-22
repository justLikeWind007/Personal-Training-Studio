package com.jianshengfang.ptstudio.core.app.schedule;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CoachScheduleService {

    private final InMemoryScheduleStore store;

    public CoachScheduleService(InMemoryScheduleStore store) {
        this.store = store;
    }

    public List<InMemoryScheduleStore.CoachData> listCoaches(String tenantId, String storeId) {
        return store.coachById().values().stream()
                .filter(coach -> coach.tenantId().equals(tenantId) && coach.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryScheduleStore.CoachData::id))
                .toList();
    }

    public InMemoryScheduleStore.CoachData createCoach(CreateCoachCommand command) {
        long id = store.nextCoachId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.CoachData coach = new InMemoryScheduleStore.CoachData(
                id,
                command.tenantId(),
                command.storeId(),
                command.name(),
                command.mobile(),
                command.level(),
                command.specialties(),
                "ACTIVE",
                now,
                now
        );
        store.coachById().put(id, coach);
        return coach;
    }

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

    public List<InMemoryScheduleStore.SlotData> listCoachSlots(Long coachId, String tenantId, String storeId) {
        getCoach(coachId, tenantId, storeId).orElseThrow(() -> new IllegalArgumentException("教练不存在"));
        return store.listSlotsByCoach(coachId).stream()
                .filter(slot -> slot.tenantId().equals(tenantId) && slot.storeId().equals(storeId))
                .toList();
    }

    public InMemoryScheduleStore.SlotData createCoachSlot(Long coachId, CreateSlotCommand command) {
        getCoach(coachId, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("教练不存在"));

        if (!command.endTime().isAfter(command.startTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        boolean duplicated = store.listSlotsByCoach(coachId).stream()
                .anyMatch(slot -> slot.slotDate().equals(command.slotDate())
                        && slot.startTime().equals(command.startTime())
                        && slot.endTime().equals(command.endTime())
                        && slot.tenantId().equals(command.tenantId())
                        && slot.storeId().equals(command.storeId()));
        if (duplicated) {
            throw new IllegalArgumentException("该时段已存在");
        }

        long id = store.nextSlotId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryScheduleStore.SlotData slot = new InMemoryScheduleStore.SlotData(
                id,
                coachId,
                command.tenantId(),
                command.storeId(),
                command.slotDate(),
                command.startTime(),
                command.endTime(),
                command.capacity(),
                0,
                "OPEN",
                now,
                now
        );
        store.slotById().put(id, slot);
        return slot;
    }

    public record CreateCoachCommand(String tenantId,
                                     String storeId,
                                     String name,
                                     String mobile,
                                     String level,
                                     String specialties) {
    }

    public record CreateSlotCommand(String tenantId,
                                    String storeId,
                                    LocalDate slotDate,
                                    LocalTime startTime,
                                    LocalTime endTime,
                                    int capacity) {
    }
}
