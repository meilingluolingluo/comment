package com.mll.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdGeneratorService {

    // 定义起始时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 使用较短的日期格式
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmm");

    public String generateUniqueId(String keyPrefix) {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(DATE_FORMATTER);

        // Redis key
        String key = keyPrefix + ":" + dateTime;

        // 原子递增
        Long increment = stringRedisTemplate.opsForValue().increment(key);

        // 生成唯一ID，增量位数减少为4位
        return dateTime + String.format("%04d", increment);
    }
}
