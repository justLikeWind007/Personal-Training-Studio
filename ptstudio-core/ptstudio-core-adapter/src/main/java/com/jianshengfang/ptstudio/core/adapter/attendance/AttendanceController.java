package com.jianshengfang.ptstudio.core.adapter.attendance;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.attendance.AttendanceService;
import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
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

import java.util.List;

@RestController
@Validated
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/api/checkins")
    @AuditAction(module = "CHECKIN", action = "CREATE")
    public InMemoryAttendanceStore.CheckinData createCheckin(@Valid @RequestBody CheckinRequest request) {
        TenantStoreContext context = requireContext();
        return attendanceService.checkin(new AttendanceService.CheckinCommand(
                context.tenantId(),
                context.storeId(),
                request.reservationId(),
                request.checkinChannel(),
                request.operatorUserId()
        ));
    }

    @GetMapping("/api/checkins")
    public List<InMemoryAttendanceStore.CheckinData> listCheckins() {
        TenantStoreContext context = requireContext();
        return attendanceService.listCheckins(context.tenantId(), context.storeId());
    }

    @PostMapping("/api/consumptions")
    @AuditAction(module = "CONSUMPTION", action = "CREATE")
    public InMemoryAttendanceStore.ConsumptionData createConsumption(@Valid @RequestBody ConsumptionRequest request) {
        TenantStoreContext context = requireContext();
        return attendanceService.consume(new AttendanceService.ConsumeCommand(
                context.tenantId(),
                context.storeId(),
                request.reservationId(),
                request.sessionsDelta(),
                request.idemKey(),
                request.operatorUserId()
        ));
    }

    @GetMapping("/api/consumptions")
    public List<InMemoryAttendanceStore.ConsumptionData> listConsumptions() {
        TenantStoreContext context = requireContext();
        return attendanceService.listConsumptions(context.tenantId(), context.storeId());
    }

    @PostMapping("/api/consumptions/{id}/reverse")
    @AuditAction(module = "CONSUMPTION", action = "REVERSE")
    public InMemoryAttendanceStore.ConsumptionData reverse(@PathVariable Long id,
                                                           @Valid @RequestBody ReverseConsumptionRequest request) {
        TenantStoreContext context = requireContext();
        return attendanceService.reverse(id, context.tenantId(), context.storeId(), request.operatorUserId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CheckinRequest(@NotNull Long reservationId,
                                 @NotBlank String checkinChannel,
                                 @NotNull Long operatorUserId) {
    }

    public record ConsumptionRequest(@NotNull Long reservationId,
                                     @Positive Integer sessionsDelta,
                                     @NotBlank String idemKey,
                                     @NotNull Long operatorUserId) {
    }

    public record ReverseConsumptionRequest(@NotNull Long operatorUserId) {
    }
}
