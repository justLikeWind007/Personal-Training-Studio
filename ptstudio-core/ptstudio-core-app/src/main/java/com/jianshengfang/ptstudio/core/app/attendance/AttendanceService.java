package com.jianshengfang.ptstudio.core.app.attendance;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import com.jianshengfang.ptstudio.core.app.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, ScheduleRepository scheduleRepository) {
        this.attendanceRepository = attendanceRepository;
        this.scheduleRepository = scheduleRepository;
    }

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
}
