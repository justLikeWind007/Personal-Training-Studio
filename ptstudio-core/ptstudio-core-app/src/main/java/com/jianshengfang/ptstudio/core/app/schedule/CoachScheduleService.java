package com.jianshengfang.ptstudio.core.app.schedule;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CoachScheduleService {

    private final ScheduleRepository scheduleRepository;

    public CoachScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public List<InMemoryScheduleStore.CoachData> listCoaches(String tenantId, String storeId) {
        return scheduleRepository.listCoaches(tenantId, storeId);
    }

    public InMemoryScheduleStore.CoachData createCoach(CreateCoachCommand command) {
        return scheduleRepository.createCoach(
                command.tenantId(),
                command.storeId(),
                command.name(),
                command.mobile(),
                command.level(),
                command.specialties()
        );
    }

    public Optional<InMemoryScheduleStore.CoachData> getCoach(Long coachId, String tenantId, String storeId) {
        return scheduleRepository.getCoach(coachId, tenantId, storeId);
    }

    public List<InMemoryScheduleStore.SlotData> listCoachSlots(Long coachId, String tenantId, String storeId) {
        getCoach(coachId, tenantId, storeId).orElseThrow(() -> new IllegalArgumentException("教练不存在"));
        return scheduleRepository.listCoachSlots(coachId, tenantId, storeId);
    }

    public InMemoryScheduleStore.SlotData createCoachSlot(Long coachId, CreateSlotCommand command) {
        getCoach(coachId, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("教练不存在"));

        if (!command.endTime().isAfter(command.startTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        boolean duplicated = scheduleRepository.listCoachSlots(coachId, command.tenantId(), command.storeId()).stream()
                .anyMatch(slot -> slot.slotDate().equals(command.slotDate())
                        && slot.startTime().equals(command.startTime())
                        && slot.endTime().equals(command.endTime())
                );
        if (duplicated) {
            throw new IllegalArgumentException("该时段已存在");
        }

        return scheduleRepository.createSlot(
                coachId,
                command.tenantId(),
                command.storeId(),
                command.slotDate(),
                command.startTime(),
                command.endTime(),
                command.capacity()
        );
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
