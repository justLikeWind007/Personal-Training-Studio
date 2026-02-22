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
import org.springframework.web.bind.annotation.RequestParam;

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

    @PostMapping("/api/checkins/makeups")
    @AuditAction(module = "CHECKIN", action = "MAKEUP_SUBMIT")
    public InMemoryAttendanceStore.ApprovalRequestData submitMakeupApproval(
            @Valid @RequestBody SubmitMakeupApprovalRequest request) {
        TenantStoreContext context = requireContext();
        return attendanceService.submitMakeupApproval(new AttendanceService.SubmitMakeupApprovalCommand(
                context.tenantId(),
                context.storeId(),
                request.reservationId(),
                request.reason(),
                request.submittedBy()
        ));
    }

    @GetMapping("/api/checkins/makeups")
    public List<InMemoryAttendanceStore.ApprovalRequestData> listMakeupApprovals(
            @RequestParam(value = "status", required = false) String status) {
        TenantStoreContext context = requireContext();
        return attendanceService.listMakeupApprovals(context.tenantId(), context.storeId(), status);
    }

    @PostMapping("/api/checkins/makeups/{id}/approve")
    @AuditAction(module = "CHECKIN", action = "MAKEUP_APPROVE")
    public InMemoryAttendanceStore.ApprovalRequestData approveMakeupApproval(
            @PathVariable Long id,
            @Valid @RequestBody MakeupApproveRequest request) {
        TenantStoreContext context = requireContext();
        return attendanceService.approveMakeupApproval(new AttendanceService.ApproveMakeupApprovalCommand(
                context.tenantId(),
                context.storeId(),
                id,
                request.approvedBy()
        ));
    }

    @PostMapping("/api/checkins/makeups/{id}/reject")
    @AuditAction(module = "CHECKIN", action = "MAKEUP_REJECT")
    public InMemoryAttendanceStore.ApprovalRequestData rejectMakeupApproval(
            @PathVariable Long id,
            @Valid @RequestBody MakeupRejectRequest request) {
        TenantStoreContext context = requireContext();
        return attendanceService.rejectMakeupApproval(new AttendanceService.RejectMakeupApprovalCommand(
                context.tenantId(),
                context.storeId(),
                id,
                request.rejectReason(),
                request.approvedBy()
        ));
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

    public record SubmitMakeupApprovalRequest(@NotNull Long reservationId,
                                              @NotBlank String reason,
                                              @NotNull Long submittedBy) {
    }

    public record MakeupApproveRequest(@NotNull Long approvedBy) {
    }

    public record MakeupRejectRequest(@NotBlank String rejectReason,
                                      @NotNull Long approvedBy) {
    }
}
