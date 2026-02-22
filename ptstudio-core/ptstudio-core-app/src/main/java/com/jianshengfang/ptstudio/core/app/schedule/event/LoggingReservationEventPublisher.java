package com.jianshengfang.ptstudio.core.app.schedule.event;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!mq")
public class LoggingReservationEventPublisher implements ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingReservationEventPublisher.class);

    @Override
    public void publishCreated(InMemoryScheduleStore.ReservationData reservation) {
        log.info("reservation.event.created reservationNo={}, memberId={}, slotId={}, tenant={}, store={}",
                reservation.reservationNo(), reservation.memberId(), reservation.slotId(),
                reservation.tenantId(), reservation.storeId());
    }

    @Override
    public void publishCanceled(InMemoryScheduleStore.ReservationData reservation) {
        log.info("reservation.event.canceled reservationNo={}, memberId={}, slotId={}, reason={}, tenant={}, store={}",
                reservation.reservationNo(), reservation.memberId(), reservation.slotId(), reservation.cancelReason(),
                reservation.tenantId(), reservation.storeId());
    }
}
