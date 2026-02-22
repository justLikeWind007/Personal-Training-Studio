package com.jianshengfang.ptstudio.core.app.attendance;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository {

    boolean existsCheckedInReservation(String tenantId, String storeId, Long reservationId);

    InMemoryAttendanceStore.CheckinData createCheckin(String tenantId,
                                                      String storeId,
                                                      Long reservationId,
                                                      Long memberId,
                                                      String checkinChannel,
                                                      Long operatorUserId,
                                                      OffsetDateTime checkinTime);

    List<InMemoryAttendanceStore.CheckinData> listCheckins(String tenantId, String storeId);

    Optional<InMemoryAttendanceStore.ConsumptionData> getConsumptionByIdemKey(String tenantId,
                                                                              String storeId,
                                                                              String idemKey);

    InMemoryAttendanceStore.ConsumptionData createConsumption(String tenantId,
                                                              String storeId,
                                                              Long reservationId,
                                                              Long memberId,
                                                              Integer sessionsDelta,
                                                              String idemKey,
                                                              Long operatorUserId,
                                                              OffsetDateTime consumeTime);

    List<InMemoryAttendanceStore.ConsumptionData> listConsumptions(String tenantId, String storeId);

    Optional<InMemoryAttendanceStore.ConsumptionData> getConsumption(Long consumptionId, String tenantId, String storeId);

    InMemoryAttendanceStore.ConsumptionData updateConsumptionStatus(Long consumptionId,
                                                                    String tenantId,
                                                                    String storeId,
                                                                    String status,
                                                                    Long operatorUserId,
                                                                    OffsetDateTime updatedAt);
}
