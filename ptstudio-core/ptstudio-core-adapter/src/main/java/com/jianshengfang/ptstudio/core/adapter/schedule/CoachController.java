package com.jianshengfang.ptstudio.core.adapter.schedule;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.schedule.CoachScheduleService;
import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/coaches")
@Validated
public class CoachController {

    private final CoachScheduleService coachScheduleService;

    public CoachController(CoachScheduleService coachScheduleService) {
        this.coachScheduleService = coachScheduleService;
    }

    @GetMapping
    public List<InMemoryScheduleStore.CoachData> listCoaches() {
        TenantStoreContext context = requireContext();
        return coachScheduleService.listCoaches(context.tenantId(), context.storeId());
    }

    @PostMapping
    @AuditAction(module = "SCHEDULE_COACH", action = "CREATE_COACH")
    public InMemoryScheduleStore.CoachData createCoach(@Valid @RequestBody CreateCoachRequest request) {
        TenantStoreContext context = requireContext();
        return coachScheduleService.createCoach(new CoachScheduleService.CreateCoachCommand(
                context.tenantId(),
                context.storeId(),
                request.name(),
                request.mobile(),
                request.level(),
                request.specialties()
        ));
    }

    @GetMapping("/{id}/slots")
    public List<InMemoryScheduleStore.SlotData> listCoachSlots(@PathVariable("id") Long coachId) {
        TenantStoreContext context = requireContext();
        return coachScheduleService.listCoachSlots(coachId, context.tenantId(), context.storeId());
    }

    @PostMapping("/{id}/slots")
    @AuditAction(module = "SCHEDULE_SLOT", action = "CREATE_SLOT")
    public InMemoryScheduleStore.SlotData createSlot(@PathVariable("id") Long coachId,
                                                     @Valid @RequestBody CreateSlotRequest request) {
        TenantStoreContext context = requireContext();
        return coachScheduleService.createCoachSlot(coachId, new CoachScheduleService.CreateSlotCommand(
                context.tenantId(),
                context.storeId(),
                request.slotDate(),
                request.startTime(),
                request.endTime(),
                request.capacity()
        ));
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateCoachRequest(@NotBlank String name,
                                     @NotBlank String mobile,
                                     @NotBlank String level,
                                     @NotBlank String specialties) {
    }

    public record CreateSlotRequest(@NotNull LocalDate slotDate,
                                    @NotNull LocalTime startTime,
                                    @NotNull LocalTime endTime,
                                    @Positive int capacity) {
    }
}
