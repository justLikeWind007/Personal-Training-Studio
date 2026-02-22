package com.jianshengfang.ptstudio.core.app.attendance;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AttendanceService {

    private final InMemoryAttendanceStore attendanceStore;
    private final InMemoryScheduleStore scheduleStore;

    public AttendanceService(InMemoryAttendanceStore attendanceStore, InMemoryScheduleStore scheduleStore) {
        this.attendanceStore = attendanceStore;
        this.scheduleStore = scheduleStore;
    }

    public InMemoryAttendanceStore.CheckinData checkin(CheckinCommand command) {
        InMemoryScheduleStore.ReservationData reservation = getBookedReservation(
                command.reservationId(), command.tenantId(), command.storeId());

        boolean exists = attendanceStore.checkinById().values().stream()
                .anyMatch(checkin -> checkin.reservationId().equals(command.reservationId())
                        && checkin.tenantId().equals(command.tenantId())
                        && checkin.storeId().equals(command.storeId())
                        && checkin.status().equals("CHECKED_IN"));
        if (exists) {
            throw new IllegalArgumentException("该预约已签到");
        }

        long id = attendanceStore.nextCheckinId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryAttendanceStore.CheckinData checkin = new InMemoryAttendanceStore.CheckinData(
                id,
                reservation.id(),
                reservation.memberId(),
                command.tenantId(),
                command.storeId(),
                command.checkinChannel(),
                command.operatorUserId(),
                "CHECKED_IN",
                now,
                now
        );
        attendanceStore.checkinById().put(id, checkin);
        return checkin;
    }

    public List<InMemoryAttendanceStore.CheckinData> listCheckins(String tenantId, String storeId) {
        return attendanceStore.checkinById().values().stream()
                .filter(checkin -> checkin.tenantId().equals(tenantId) && checkin.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryAttendanceStore.CheckinData::id))
                .toList();
    }

    public InMemoryAttendanceStore.ConsumptionData consume(ConsumeCommand command) {
        if (command.sessionsDelta() <= 0) {
            throw new IllegalArgumentException("课消次数必须大于0");
        }
        InMemoryScheduleStore.ReservationData reservation = getBookedReservation(
                command.reservationId(), command.tenantId(), command.storeId());

        Long existingConsumptionId = attendanceStore.consumptionIdByIdemKey().get(command.idemKey());
        if (existingConsumptionId != null) {
            InMemoryAttendanceStore.ConsumptionData existing = attendanceStore.consumptionById().get(existingConsumptionId);
            if (existing != null) {
                return existing;
            }
        }

        long id = attendanceStore.nextConsumptionId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryAttendanceStore.ConsumptionData consumption = new InMemoryAttendanceStore.ConsumptionData(
                id,
                reservation.id(),
                reservation.memberId(),
                command.sessionsDelta(),
                command.tenantId(),
                command.storeId(),
                command.idemKey(),
                command.operatorUserId(),
                "CONSUMED",
                now,
                now,
                now
        );
        attendanceStore.consumptionById().put(id, consumption);
        attendanceStore.consumptionIdByIdemKey().put(command.idemKey(), id);
        return consumption;
    }

    public List<InMemoryAttendanceStore.ConsumptionData> listConsumptions(String tenantId, String storeId) {
        return attendanceStore.consumptionById().values().stream()
                .filter(consumption -> consumption.tenantId().equals(tenantId) && consumption.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryAttendanceStore.ConsumptionData::id))
                .toList();
    }

    public InMemoryAttendanceStore.ConsumptionData reverse(Long consumptionId,
                                                           String tenantId,
                                                           String storeId,
                                                           Long operatorUserId) {
        InMemoryAttendanceStore.ConsumptionData existing = attendanceStore.consumptionById().get(consumptionId);
        if (existing == null || !existing.tenantId().equals(tenantId) || !existing.storeId().equals(storeId)) {
            throw new IllegalArgumentException("课消记录不存在");
        }
        if (!existing.status().equals("CONSUMED")) {
            throw new IllegalArgumentException("当前状态不可冲正");
        }

        InMemoryAttendanceStore.ConsumptionData reversed = new InMemoryAttendanceStore.ConsumptionData(
                existing.id(),
                existing.reservationId(),
                existing.memberId(),
                existing.sessionsDelta(),
                existing.tenantId(),
                existing.storeId(),
                existing.idemKey(),
                operatorUserId,
                "REVERSED",
                existing.consumeTime(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        attendanceStore.consumptionById().put(existing.id(), reversed);
        return reversed;
    }

    private InMemoryScheduleStore.ReservationData getBookedReservation(Long reservationId,
                                                                       String tenantId,
                                                                       String storeId) {
        InMemoryScheduleStore.ReservationData reservation = scheduleStore.reservationById().get(reservationId);
        if (reservation == null || !reservation.tenantId().equals(tenantId) || !reservation.storeId().equals(storeId)) {
            throw new IllegalArgumentException("预约不存在");
        }
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
