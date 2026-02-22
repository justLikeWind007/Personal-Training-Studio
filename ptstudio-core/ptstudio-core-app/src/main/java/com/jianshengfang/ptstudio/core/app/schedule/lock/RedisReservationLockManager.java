package com.jianshengfang.ptstudio.core.app.schedule.lock;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Profile("redis")
public class RedisReservationLockManager implements ReservationLockManager {

    private static final String PREFIX = "ptstudio:reserve:lock:";

    private final StringRedisTemplate redisTemplate;

    public RedisReservationLockManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LockToken tryLock(String key, Duration ttl) {
        String lockKey = PREFIX + key;
        String value = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, value, ttl);
        if (Boolean.TRUE.equals(ok)) {
            return new LockToken(lockKey, value);
        }
        return null;
    }

    @Override
    public void unlock(LockToken token) {
        if (token == null) {
            return;
        }
        String current = redisTemplate.opsForValue().get(token.key());
        if (token.value().equals(current)) {
            redisTemplate.delete(token.key());
        }
    }
}
