package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxEventPo;
import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxReservationEventMapper;
import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxReservationEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReservationEventPublisherTest {

    @Test
    void shouldBuildOutboxPayloadWhenPublishingCreatedAndCanceled() throws Exception {
        MysqlOutboxReservationEventMapper mapper = mock(MysqlOutboxReservationEventMapper.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MysqlOutboxReservationEventPublisher publisher = new MysqlOutboxReservationEventPublisher(
                mapper, "ptstudio.reservation.changed", "reservation_created", "reservation_canceled");

        InMemoryScheduleStore.ReservationData created = new InMemoryScheduleStore.ReservationData(
                101L,
                "R202602220001",
                2001L,
                3001L,
                4001L,
                "1001",
                "2001",
                "BOOKED",
                null,
                null,
                OffsetDateTime.parse("2026-02-22T10:00:00+08:00"),
                OffsetDateTime.parse("2026-02-22T10:00:00+08:00")
        );
        InMemoryScheduleStore.ReservationData canceled = new InMemoryScheduleStore.ReservationData(
                101L,
                "R202602220001",
                2001L,
                3001L,
                4001L,
                "1001",
                "2001",
                "CANCELED",
                "member_plan_change",
                OffsetDateTime.parse("2026-02-22T11:00:00+08:00"),
                OffsetDateTime.parse("2026-02-22T10:00:00+08:00"),
                OffsetDateTime.parse("2026-02-22T11:00:00+08:00")
        );

        publisher.publishCreated(created);
        publisher.publishCanceled(canceled);

        verify(mapper, times(2)).insert(any(MysqlOutboxEventPo.class));
        List<MysqlOutboxEventPo> captured = capturePayloads(mapper);
        JsonNode createdPayload = objectMapper.readTree(captured.get(0).getPayloadJson());
        assertThat(captured.get(0).getTopic()).isEqualTo("ptstudio.reservation.changed");
        assertThat(captured.get(0).getTag()).isEqualTo("reservation_created");
        assertThat(captured.get(0).getBizType()).isEqualTo("RESERVATION");
        assertThat(captured.get(0).getBizId()).isEqualTo(101L);
        assertThat(createdPayload.path("eventType").asText()).isEqualTo("RESERVATION_CREATED");
        assertThat(createdPayload.path("reservationNo").asText()).isEqualTo("R202602220001");

        JsonNode canceledPayload = objectMapper.readTree(captured.get(1).getPayloadJson());
        assertThat(captured.get(1).getTag()).isEqualTo("reservation_canceled");
        assertThat(canceledPayload.path("eventType").asText()).isEqualTo("RESERVATION_CANCELED");
        assertThat(canceledPayload.path("cancelReason").asText()).isEqualTo("member_plan_change");
    }

    private List<MysqlOutboxEventPo> capturePayloads(MysqlOutboxReservationEventMapper mapper) {
        ArgumentCaptor<MysqlOutboxEventPo> captor = ArgumentCaptor.forClass(MysqlOutboxEventPo.class);
        verify(mapper, times(2)).insert(captor.capture());
        return captor.getAllValues();
    }
}
