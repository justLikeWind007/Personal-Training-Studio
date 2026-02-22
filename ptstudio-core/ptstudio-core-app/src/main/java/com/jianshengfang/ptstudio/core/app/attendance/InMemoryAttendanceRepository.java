package com.jianshengfang.ptstudio.core.app.attendance;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!mysql")
public class InMemoryAttendanceRepository implements AttendanceRepository {

    private final InMemoryAttendanceStore store;

    public InMemoryAttendanceRepository(InMemoryAttendanceStore store) {
        this.store = store;
    }

    @Override
    public boolean existsCheckedInReservation(String tenantId, String storeId, Long reservationId) {
        return store.checkinById().values().stream()
                .anyMatch(checkin -> checkin.reservationId().equals(reservationId)
                        && checkin.tenantId().equals(tenantId)
                        && checkin.storeId().equals(storeId)
                        && checkin.status().equals("CHECKED_IN"));
    }

    @Override
    public InMemoryAttendanceStore.CheckinData createCheckin(String tenantId, String storeId, Long reservationId,
                                                             Long memberId, String checkinChannel,
                                                             Long operatorUserId, OffsetDateTime checkinTime) {
        long id = store.nextCheckinId();
        InMemoryAttendanceStore.CheckinData checkin = new InMemoryAttendanceStore.CheckinData(
                id, reservationId, memberId, tenantId, storeId,
                checkinChannel, operatorUserId, "CHECKED_IN", checkinTime, checkinTime
        );
        store.checkinById().put(id, checkin);
        return checkin;
    }

    @Override
    public List<InMemoryAttendanceStore.CheckinData> listCheckins(String tenantId, String storeId) {
        return store.checkinById().values().stream()
                .filter(checkin -> checkin.tenantId().equals(tenantId) && checkin.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryAttendanceStore.CheckinData::id))
                .toList();
    }

    @Override
    public Optional<InMemoryAttendanceStore.ConsumptionData> getConsumptionByIdemKey(String tenantId,
                                                                                      String storeId,
                                                                                      String idemKey) {
        Long id = store.consumptionIdByIdemKey().get(idemKey);
        if (id == null) {
            return Optional.empty();
        }
        InMemoryAttendanceStore.ConsumptionData consumption = store.consumptionById().get(id);
        if (consumption == null) {
            return Optional.empty();
        }
        if (!consumption.tenantId().equals(tenantId) || !consumption.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(consumption);
    }

    @Override
    public InMemoryAttendanceStore.ConsumptionData createConsumption(String tenantId, String storeId,
                                                                     Long reservationId, Long memberId,
                                                                     Integer sessionsDelta, String idemKey,
                                                                     Long operatorUserId, OffsetDateTime consumeTime) {
        long id = store.nextConsumptionId();
        InMemoryAttendanceStore.ConsumptionData consumption = new InMemoryAttendanceStore.ConsumptionData(
                id, reservationId, memberId, sessionsDelta, tenantId, storeId,
                idemKey, operatorUserId, "CONSUMED", consumeTime, consumeTime, consumeTime
        );
        store.consumptionById().put(id, consumption);
        store.consumptionIdByIdemKey().put(idemKey, id);
        return consumption;
    }

    @Override
    public List<InMemoryAttendanceStore.ConsumptionData> listConsumptions(String tenantId, String storeId) {
        return store.consumptionById().values().stream()
                .filter(consumption -> consumption.tenantId().equals(tenantId) && consumption.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryAttendanceStore.ConsumptionData::id))
                .toList();
    }

    @Override
    public Optional<InMemoryAttendanceStore.ConsumptionData> getConsumption(Long consumptionId,
                                                                            String tenantId,
                                                                            String storeId) {
        InMemoryAttendanceStore.ConsumptionData data = store.consumptionById().get(consumptionId);
        if (data == null) {
            return Optional.empty();
        }
        if (!data.tenantId().equals(tenantId) || !data.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(data);
    }

    @Override
    public InMemoryAttendanceStore.ConsumptionData updateConsumptionStatus(Long consumptionId,
                                                                           String tenantId,
                                                                           String storeId,
                                                                           String status,
                                                                           Long operatorUserId,
                                                                           OffsetDateTime updatedAt) {
        InMemoryAttendanceStore.ConsumptionData existing = getConsumption(consumptionId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("课消记录不存在"));
        InMemoryAttendanceStore.ConsumptionData updated = new InMemoryAttendanceStore.ConsumptionData(
                existing.id(), existing.reservationId(), existing.memberId(), existing.sessionsDelta(),
                existing.tenantId(), existing.storeId(), existing.idemKey(), operatorUserId,
                status, existing.consumeTime(), existing.createdAt(), updatedAt
        );
        store.consumptionById().put(consumptionId, updated);
        return updated;
    }
}
