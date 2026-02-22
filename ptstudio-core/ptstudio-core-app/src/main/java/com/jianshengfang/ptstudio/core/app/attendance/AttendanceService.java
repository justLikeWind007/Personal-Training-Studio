package com.jianshengfang.ptstudio.core.app.attendance;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import com.jianshengfang.ptstudio.core.app.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private static final String MAKEUP_CHECKIN_BIZ_TYPE = "MAKEUP_CHECKIN";

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, ScheduleRepository scheduleRepository) {
        this.attendanceRepository = attendanceRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public InMemoryAttendanceStore.CheckinData checkin(CheckinCommand command) {
        InMemoryScheduleStore.ReservationData reservation = getBookedReservation(
                command.reservationId(), command.tenantId(), command.storeId());

        boolean exists = attendanceRepository.existsCheckedInReservation(
                command.tenantId(), command.storeId(), command.reservationId());
        if (exists) {
            throw new IllegalArgumentException("该预约已签到");
        }

        return attendanceRepository.createCheckin(
                command.tenantId(),
                command.storeId(),
                reservation.id(),
                reservation.memberId(),
                command.checkinChannel(),
                command.operatorUserId(),
                OffsetDateTime.now()
        );
    }

    public List<InMemoryAttendanceStore.CheckinData> listCheckins(String tenantId, String storeId) {
        return attendanceRepository.listCheckins(tenantId, storeId);
    }

    @Transactional
    public InMemoryAttendanceStore.ConsumptionData consume(ConsumeCommand command) {
        if (command.sessionsDelta() <= 0) {
            throw new IllegalArgumentException("课消次数必须大于0");
        }
        InMemoryScheduleStore.ReservationData reservation = getBookedReservation(
                command.reservationId(), command.tenantId(), command.storeId());

        var existing = attendanceRepository.getConsumptionByIdemKey(
                command.tenantId(), command.storeId(), command.idemKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        return attendanceRepository.createConsumption(
                command.tenantId(),
                command.storeId(),
                reservation.id(),
                reservation.memberId(),
                command.sessionsDelta(),
                command.idemKey(),
                command.operatorUserId(),
                OffsetDateTime.now()
        );
    }

    public List<InMemoryAttendanceStore.ConsumptionData> listConsumptions(String tenantId, String storeId) {
        return attendanceRepository.listConsumptions(tenantId, storeId);
    }

    @Transactional
    public InMemoryAttendanceStore.ConsumptionData reverse(Long consumptionId,
                                                           String tenantId,
                                                           String storeId,
                                                           Long operatorUserId) {
        InMemoryAttendanceStore.ConsumptionData existing = attendanceRepository.getConsumption(consumptionId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("课消记录不存在"));
        if (!existing.status().equals("CONSUMED")) {
            throw new IllegalArgumentException("当前状态不可冲正");
        }
        return attendanceRepository.updateConsumptionStatus(
                existing.id(), tenantId, storeId, "REVERSED", operatorUserId, OffsetDateTime.now());
    }

    @Transactional
    public InMemoryAttendanceStore.ApprovalRequestData submitMakeupApproval(SubmitMakeupApprovalCommand command) {
        InMemoryScheduleStore.ReservationData reservation = getBookedReservation(
                command.reservationId(), command.tenantId(), command.storeId());
        if (attendanceRepository.existsCheckedInReservation(command.tenantId(), command.storeId(), reservation.id())) {
            throw new IllegalArgumentException("该预约已签到，无需补签");
        }
        boolean exists = attendanceRepository.getApprovalRequestByBiz(
                command.tenantId(), command.storeId(), MAKEUP_CHECKIN_BIZ_TYPE, reservation.id()).isPresent();
        if (exists) {
            throw new IllegalArgumentException("该预约补签审批已存在");
        }
        return attendanceRepository.createApprovalRequest(
                command.tenantId(),
                command.storeId(),
                MAKEUP_CHECKIN_BIZ_TYPE,
                reservation.id(),
                command.reason(),
                command.submittedBy(),
                OffsetDateTime.now()
        );
    }

    public List<InMemoryAttendanceStore.ApprovalRequestData> listMakeupApprovals(String tenantId,
                                                                                  String storeId,
                                                                                  String status) {
        return attendanceRepository.listApprovalRequests(tenantId, storeId, MAKEUP_CHECKIN_BIZ_TYPE, status);
    }

    @Transactional
    public InMemoryAttendanceStore.ApprovalRequestData approveMakeupApproval(ApproveMakeupApprovalCommand command) {
        InMemoryAttendanceStore.ApprovalRequestData approval = attendanceRepository
                .getApprovalRequest(command.approvalId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("审批单不存在"));
        if (!MAKEUP_CHECKIN_BIZ_TYPE.equals(approval.bizType())) {
            throw new IllegalArgumentException("审批业务类型不匹配");
        }
        if (!"PENDING".equals(approval.status())) {
            throw new IllegalArgumentException("审批单状态不可通过");
        }
        InMemoryScheduleStore.ReservationData reservation = getBookedReservation(
                approval.bizId(), command.tenantId(), command.storeId());
        if (!attendanceRepository.existsCheckedInReservation(command.tenantId(), command.storeId(), reservation.id())) {
            attendanceRepository.createCheckin(
                    command.tenantId(),
                    command.storeId(),
                    reservation.id(),
                    reservation.memberId(),
                    "MAKEUP_APPROVED",
                    command.approvedBy(),
                    OffsetDateTime.now()
            );
        }
        return attendanceRepository.updateApprovalRequest(
                command.tenantId(),
                command.storeId(),
                command.approvalId(),
                "APPROVED",
                command.approvedBy(),
                null,
                OffsetDateTime.now()
        );
    }

    @Transactional
    public InMemoryAttendanceStore.ApprovalRequestData rejectMakeupApproval(RejectMakeupApprovalCommand command) {
        InMemoryAttendanceStore.ApprovalRequestData approval = attendanceRepository
                .getApprovalRequest(command.approvalId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("审批单不存在"));
        if (!MAKEUP_CHECKIN_BIZ_TYPE.equals(approval.bizType())) {
            throw new IllegalArgumentException("审批业务类型不匹配");
        }
        if (!"PENDING".equals(approval.status())) {
            throw new IllegalArgumentException("审批单状态不可拒绝");
        }
        return attendanceRepository.updateApprovalRequest(
                command.tenantId(),
                command.storeId(),
                command.approvalId(),
                "REJECTED",
                command.approvedBy(),
                command.rejectReason(),
                OffsetDateTime.now()
        );
    }

    private InMemoryScheduleStore.ReservationData getBookedReservation(Long reservationId,
                                                                       String tenantId,
                                                                       String storeId) {
        InMemoryScheduleStore.ReservationData reservation = scheduleRepository
                .getReservation(reservationId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!reservation.status().equals("BOOKED")) {
            throw new IllegalArgumentException("预约状态不可用于签到/课消");
        }
        return reservation;
    }

    public record CheckinCommand(String tenantId,
                                 String storeId,
                                 Long reservationId,
                                 String checkinChannel,
                                 Long operatorUserId) {
    }

    public record ConsumeCommand(String tenantId,
                                 String storeId,
                                 Long reservationId,
                                 Integer sessionsDelta,
                                 String idemKey,
                                 Long operatorUserId) {
    }

    public record SubmitMakeupApprovalCommand(String tenantId,
                                              String storeId,
                                              Long reservationId,
                                              String reason,
                                              Long submittedBy) {
    }

    public record ApproveMakeupApprovalCommand(String tenantId,
                                               String storeId,
                                               Long approvalId,
                                               Long approvedBy) {
    }

    public record RejectMakeupApprovalCommand(String tenantId,
                                              String storeId,
                                              Long approvalId,
                                              String rejectReason,
                                              Long approvedBy) {
    }
}
