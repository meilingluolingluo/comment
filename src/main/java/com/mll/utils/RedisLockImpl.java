package com.mll.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.redisson.api.RedissonClient;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLockImpl implements ILock {
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_SEPARATOR = ":";
    private final StringRedisTemplate stringRedisTemplate;
    private final String name;
    private final String lockId;
    public RedisLockImpl(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
        this.lockId = UUID.randomUUID().toString();
    }
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static{
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
        String lockValue = lockId + ID_SEPARATOR + System.currentTimeMillis();
        // 尝试获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, lockValue, timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        String lockKey = KEY_PREFIX + name;
        String lockValue = lockId + ID_SEPARATOR + Thread.currentThread().getName();
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(lockKey),
                lockValue
        );
    }
}
