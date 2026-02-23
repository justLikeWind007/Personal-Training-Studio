package com.jianshengfang.ptstudio.core.app.attendance.event;

import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;

public interface ConsumptionEventPublisher {

    void publishConsumed(InMemoryAttendanceStore.ConsumptionData consumption);

    void publishReversed(InMemoryAttendanceStore.ConsumptionData consumption);
}
