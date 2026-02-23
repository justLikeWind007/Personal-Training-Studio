package com.jianshengfang.ptstudio.core.infrastructure.attendance.mq;

import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import com.jianshengfang.ptstudio.core.app.attendance.event.ConsumptionEventPublisher;
import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxEventPo;
import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxReservationEventMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Profile("mq & mysql")
public class MysqlOutboxConsumptionEventPublisher implements ConsumptionEventPublisher {

    private static final String BIZ_TYPE = "CONSUMPTION";

    private final MysqlOutboxReservationEventMapper mapper;
    private final String topic;
    private final String consumedTag;
    private final String reversedTag;

    public MysqlOutboxConsumptionEventPublisher(
            MysqlOutboxReservationEventMapper mapper,
            @Value("${ptstudio.messaging.consumption.topic:ptstudio.consumption.changed}") String topic,
            @Value("${ptstudio.messaging.consumption.consumed-tag:consumption_consumed}") String consumedTag,
            @Value("${ptstudio.messaging.consumption.reversed-tag:consumption_reversed}") String reversedTag) {
        this.mapper = mapper;
        this.topic = topic;
        this.consumedTag = consumedTag;
        this.reversedTag = reversedTag;
    }

    @Override
    public void publishConsumed(InMemoryAttendanceStore.ConsumptionData consumption) {
        saveOutbox(consumption, consumedTag, "CONSUMPTION_CONSUMED", consumption.consumeTime());
    }

    @Override
    public void publishReversed(InMemoryAttendanceStore.ConsumptionData consumption) {
        saveOutbox(consumption, reversedTag, "CONSUMPTION_REVERSED", consumption.updatedAt());
    }

    private void saveOutbox(InMemoryAttendanceStore.ConsumptionData consumption,
                            String tag,
                            String eventType,
                            OffsetDateTime occurredAt) {
        MysqlOutboxEventPo po = new MysqlOutboxEventPo();
        po.setTenantId(consumption.tenantId());
        po.setStoreId(consumption.storeId());
        po.setEventId(UUID.randomUUID().toString().replace("-", ""));
        po.setTopic(topic);
        po.setTag(tag);
        po.setBizType(BIZ_TYPE);
        po.setBizId(consumption.id());
        po.setPayloadJson(toPayloadJson(consumption, eventType, occurredAt));
        mapper.insert(po);
    }

    private String toPayloadJson(InMemoryAttendanceStore.ConsumptionData consumption,
                                 String eventType,
                                 OffsetDateTime occurredAt) {
        return """
                {
                  "eventType":"%s",
                  "consumptionId":%d,
                  "reservationId":%d,
                  "memberId":%d,
                  "sessionsDelta":%d,
                  "status":"%s",
                  "idemKey":"%s",
                  "tenantId":"%s",
                  "storeId":"%s",
                  "occurredAt":"%s"
                }
                """.formatted(
                escape(eventType),
                consumption.id(),
                consumption.reservationId(),
                consumption.memberId(),
                consumption.sessionsDelta(),
                escape(consumption.status()),
                escape(consumption.idemKey()),
                escape(consumption.tenantId()),
                escape(consumption.storeId()),
                occurredAt
        ).replace("\n", "").replace("  ", "");
    }

    private String escape(String source) {
        if (source == null) {
            return "";
        }
        return source.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
