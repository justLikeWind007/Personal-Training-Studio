package com.jianshengfang.ptstudio.core.adapter.schedule;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import com.jianshengfang.ptstudio.core.app.schedule.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/api/slots/available")
    public List<InMemoryScheduleStore.SlotData> availableSlots(@RequestParam(name = "coachId", required = false) Long coachId,
                                                                @RequestParam(name = "slotDate", required = false)
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate) {
        TenantStoreContext context = requireContext();
        return reservationService.listAvailableSlots(context.tenantId(), context.storeId(), coachId, slotDate);
    }

    @PostMapping("/api/reservations")
    @AuditAction(module = "RESERVATION", action = "CREATE")
    public InMemoryScheduleStore.ReservationData create(@Valid @RequestBody CreateReservationRequest request) {
        TenantStoreContext context = requireContext();
        return reservationService.createReservation(new ReservationService.CreateReservationCommand(
                context.tenantId(),
                context.storeId(),
                request.memberId(),
                request.slotId()
        ));
    }

    @GetMapping("/api/reservations")
    public List<InMemoryScheduleStore.ReservationData> list(@RequestParam(name = "memberId", required = false) Long memberId,
                                                             @RequestParam(name = "coachId", required = false) Long coachId,
                                                             @RequestParam(name = "status", required = false) String status) {
        TenantStoreContext context = requireContext();
        return reservationService.listReservations(context.tenantId(), context.storeId(), memberId, coachId, status);
    }

    @GetMapping("/api/reservations/{id}")
    public InMemoryScheduleStore.ReservationData detail(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return reservationService.getReservation(id, context.tenantId(), context.storeId())
                .orElseThrow(() -> new IllegalArgumentException("预约单不存在"));
    }

    @PostMapping("/api/reservations/{id}/cancel")
    @AuditAction(module = "RESERVATION", action = "CANCEL")
    public InMemoryScheduleStore.ReservationData cancel(@PathVariable Long id,
                                                        @Valid @RequestBody CancelReservationRequest request) {
        TenantStoreContext context = requireContext();
        return reservationService.cancelReservation(id, context.tenantId(), context.storeId(), request.reason());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateReservationRequest(@NotNull Long memberId, @NotNull Long slotId) {
    }

    public record CancelReservationRequest(@NotBlank String reason) {
    }
}
