package com.jianshengfang.ptstudio.core.infrastructure.schedule.mq;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import com.jianshengfang.ptstudio.core.app.schedule.event.ReservationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Profile("mq & mysql")
public class MysqlOutboxReservationEventPublisher implements ReservationEventPublisher {

    private static final String BIZ_TYPE = "RESERVATION";

    private final MysqlOutboxReservationEventMapper mapper;
    private final String reservationTopic;
    private final String createdTag;
    private final String canceledTag;

    public MysqlOutboxReservationEventPublisher(
            MysqlOutboxReservationEventMapper mapper,
            @Value("${ptstudio.messaging.reservation.topic:ptstudio.reservation.changed}") String reservationTopic,
            @Value("${ptstudio.messaging.reservation.created-tag:reservation_created}") String createdTag,
            @Value("${ptstudio.messaging.reservation.canceled-tag:reservation_canceled}") String canceledTag) {
        this.mapper = mapper;
        this.reservationTopic = reservationTopic;
        this.createdTag = createdTag;
        this.canceledTag = canceledTag;
    }

    @Override
    public void publishCreated(InMemoryScheduleStore.ReservationData reservation) {
        saveOutbox(
                reservation,
                createdTag,
                "RESERVATION_CREATED",
                reservation.createdAt()
        );
    }

    @Override
    public void publishCanceled(InMemoryScheduleStore.ReservationData reservation) {
        saveOutbox(
                reservation,
                canceledTag,
                "RESERVATION_CANCELED",
                reservation.cancelAt() == null ? OffsetDateTime.now() : reservation.cancelAt()
        );
    }

    private void saveOutbox(InMemoryScheduleStore.ReservationData reservation,
                            String tag,
                            String eventType,
                            OffsetDateTime occurredAt) {
        MysqlOutboxEventPo po = new MysqlOutboxEventPo();
        po.setTenantId(reservation.tenantId());
        po.setStoreId(reservation.storeId());
        po.setEventId(UUID.randomUUID().toString().replace("-", ""));
        po.setTopic(reservationTopic);
        po.setTag(tag);
        po.setBizType(BIZ_TYPE);
        po.setBizId(reservation.id());
        po.setPayloadJson(toPayloadJson(reservation, eventType, occurredAt));
        mapper.insert(po);
    }

    private String toPayloadJson(InMemoryScheduleStore.ReservationData reservation,
                                 String eventType,
                                 OffsetDateTime occurredAt) {
        return """
                {
                  "eventType":"%s",
                  "reservationId":%d,
                  "reservationNo":"%s",
                  "memberId":%d,
                  "coachId":%d,
                  "slotId":%d,
                  "status":"%s",
                  "cancelReason":%s,
                  "tenantId":"%s",
                  "storeId":"%s",
                  "occurredAt":"%s"
                }
                """.formatted(
                escape(eventType),
                reservation.id(),
                escape(reservation.reservationNo()),
                reservation.memberId(),
                reservation.coachId(),
                reservation.slotId(),
                escape(reservation.status()),
                reservation.cancelReason() == null ? "null" : "\"" + escape(reservation.cancelReason()) + "\"",
                escape(reservation.tenantId()),
                escape(reservation.storeId()),
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
