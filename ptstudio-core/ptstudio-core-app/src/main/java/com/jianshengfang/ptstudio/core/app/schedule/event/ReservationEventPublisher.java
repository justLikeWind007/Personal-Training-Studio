package com.jianshengfang.ptstudio.core.app.schedule.event;

import com.jianshengfang.ptstudio.core.app.schedule.InMemoryScheduleStore;

public interface ReservationEventPublisher {

    void publishCreated(InMemoryScheduleStore.ReservationData reservation);

    void publishCanceled(InMemoryScheduleStore.ReservationData reservation);
}
