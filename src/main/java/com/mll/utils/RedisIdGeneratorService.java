package com.mll.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdGeneratorService {
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generateUniqueId(String keyPrefix) {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(DATE_FORMATTER);

        // Redis key
        String key = keyPrefix + ":" + dateTime;

        // 原子递增
        Long increment = stringRedisTemplate.opsForValue().increment(key);

        // 生成唯一ID
        return dateTime + String.format("%06d", increment);
    }
}
