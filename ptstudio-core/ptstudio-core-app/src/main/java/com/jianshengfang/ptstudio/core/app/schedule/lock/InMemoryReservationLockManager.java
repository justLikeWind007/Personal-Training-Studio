package com.jianshengfang.ptstudio.core.app.schedule.lock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!redis")
public class InMemoryReservationLockManager implements ReservationLockManager {

    private final Map<String, LockHolder> locks = new ConcurrentHashMap<>();

    @Override
    public LockToken tryLock(String key, Duration ttl) {
        Instant now = Instant.now();
        locks.entrySet().removeIf(e -> e.getValue().expireAt().isBefore(now));

        String value = UUID.randomUUID().toString();
        LockHolder holder = new LockHolder(value, now.plus(ttl));
        LockHolder existing = locks.putIfAbsent(key, holder);
        if (existing != null) {
            return null;
        }
        return new LockToken(key, value);
    }

    @Override
    public void unlock(LockToken token) {
        if (token == null) {
            return;
        }
        locks.computeIfPresent(token.key(), (k, holder) -> holder.value().equals(token.value()) ? null : holder);
    }

    private record LockHolder(String value, Instant expireAt) {
    }
}
