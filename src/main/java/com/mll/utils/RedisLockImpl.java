package com.mll.utils;

import com.mll.utils.ILock;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisLockImpl implements ILock {
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_SEPARATOR = ":";

    private StringRedisTemplate stringRedisTemplate;
    private String name;

    public RedisLockImpl(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
        // 获取当前线程ID
        long threadId = Thread.currentThread().getId();
        String lockValue = threadId + ID_SEPARATOR + System.currentTimeMillis();

        // 尝试获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, lockValue, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 获取当前线程ID
        long threadId = Thread.currentThread().getId();
        String lockKey = KEY_PREFIX + name;
        String lockValue = stringRedisTemplate.opsForValue().get(lockKey);

        if (lockValue != null) {
            // 提取存储的线程ID
            String[] parts = lockValue.split(ID_SEPARATOR);
            if (parts.length == 2) {
                long storedThreadId = Long.parseLong(parts[0]);

                // 只有持有锁的线程才能释放锁
                if (storedThreadId == threadId) {
                    stringRedisTemplate.delete(lockKey);
                }
            }
        }
    }
}
