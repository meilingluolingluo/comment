package com.mll.utils;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;
    @Resource
    private BloomFilterHelper bloomFilterHelper;

    @Autowired
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 存储缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 存储逻辑过期缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public void setWithLogicalExpire(String key, String value, long timeout, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 查询数据并通过缓存穿透的方式处理，缓存空结果
     * @param keyPrefix 缓存键前缀
     * @param id 数据ID
     * @param type 返回类型
     * @param dbFallback 数据库查询回退函数
     * @param time 缓存过期时间
     * @param unit 缓存过期时间单位
     * @param <R> 返回类型
     * @return 查询结果
     */
    public <R> R queryWithPassThrough(String keyPrefix, Long id, Class<R> type, Function<Long, R> dbFallback, Long time, TimeUnit unit) {
        String cacheKey = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            if (json.equals("null")) {
                return null;
            }
            return JSONUtil.toBean(json, type);
        }

        R result = dbFallback.apply(id);
        if (result == null) {
            stringRedisTemplate.opsForValue().set(cacheKey, "null", time, unit);
            return null;
        } else {
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), time, unit);
        }
        //this.set(cacheKey, JSONUtil.toJsonStr(result), time, unit);
        return result;
    }
    public <R> R queryWithMutex(String keyPrefix, Long id, Class<R> type, Function<Long, R> dbFallback, Long time, TimeUnit unit) {
        // 布隆过滤器检查
        if (!bloomFilterHelper.mightContain(id)) {
            return null;
        }
        String cacheKey = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            if (json.equals("null")) {
                return null;
            }
            return JSONUtil.toBean(json, type);
        }

        // 获取锁
        String lockKey = RedisConstants.LOGIN_CODE_KEY + id;
        boolean isLocked = tryLock(lockKey, 10);
        if (!isLocked) {
            // 未能获取锁，等待并重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
        }

        try {
            json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                if (json.equals("null")) {
                    return null;
                }
                return JSONUtil.toBean(json, type);
            }

            R result = dbFallback.apply(id);
            if (result == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, "null", time, unit);
                return null;
            } else {
                stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), time, unit);
            }
            return result;
        } finally {
            releaseLock(lockKey);
        }
    }
    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }


    /**
     * 删除缓存
     * @param key 缓存键
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 尝试获取分布式锁
     * @param key 锁的键
     * @param timeoutSeconds 锁的过期时间（秒）
     * @return 是否获取成功
     */
    private boolean tryLock(String key, long timeoutSeconds) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", timeoutSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }


    /**
     * 释放分布式锁
     * @param key 锁的键
     */
    public void releaseLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
