package com.jianshengfang.ptstudio.core.app.attendance.event;

import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!mq | !mysql")
public class LoggingConsumptionEventPublisher implements ConsumptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingConsumptionEventPublisher.class);

    @Override
    public void publishConsumed(InMemoryAttendanceStore.ConsumptionData consumption) {
        log.info("consumption.event.consumed consumptionId={}, reservationId={}, memberId={}, tenant={}, store={}",
                consumption.id(), consumption.reservationId(), consumption.memberId(),
                consumption.tenantId(), consumption.storeId());
    }

    @Override
    public void publishReversed(InMemoryAttendanceStore.ConsumptionData consumption) {
        log.info("consumption.event.reversed consumptionId={}, reservationId={}, memberId={}, tenant={}, store={}",
                consumption.id(), consumption.reservationId(), consumption.memberId(),
                consumption.tenantId(), consumption.storeId());
    }
}
