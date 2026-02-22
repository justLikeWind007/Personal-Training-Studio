package com.jianshengfang.ptstudio.core.app.schedule.lock;

import java.time.Duration;

public interface ReservationLockManager {

    LockToken tryLock(String key, Duration ttl);

    void unlock(LockToken token);

    record LockToken(String key, String value) {
    }
}
