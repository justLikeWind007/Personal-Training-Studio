package com.jianshengfang.ptstudio.core.app.attendance;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryAttendanceStore {

    private final AtomicLong checkinIdGenerator = new AtomicLong(1);
    private final AtomicLong consumptionIdGenerator = new AtomicLong(1);

    private final Map<Long, CheckinData> checkinById = new ConcurrentHashMap<>();
    private final Map<Long, ConsumptionData> consumptionById = new ConcurrentHashMap<>();
    private final Map<String, Long> consumptionIdByIdemKey = new ConcurrentHashMap<>();

    public long nextCheckinId() {
        return checkinIdGenerator.getAndIncrement();
    }

    public long nextConsumptionId() {
        return consumptionIdGenerator.getAndIncrement();
    }

    public Map<Long, CheckinData> checkinById() {
        return checkinById;
    }

    public Map<Long, ConsumptionData> consumptionById() {
        return consumptionById;
    }

    public Map<String, Long> consumptionIdByIdemKey() {
        return consumptionIdByIdemKey;
    }

    public record CheckinData(Long id,
                              Long reservationId,
                              Long memberId,
                              String tenantId,
                              String storeId,
                              String checkinChannel,
                              Long operatorUserId,
                              String status,
                              OffsetDateTime checkinTime,
                              OffsetDateTime createdAt) {
    }

    public record ConsumptionData(Long id,
                                  Long reservationId,
                                  Long memberId,
                                  Integer sessionsDelta,
                                  String tenantId,
                                  String storeId,
                                  String idemKey,
                                  Long operatorUserId,
                                  String status,
                                  OffsetDateTime consumeTime,
                                  OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt) {
    }
}
