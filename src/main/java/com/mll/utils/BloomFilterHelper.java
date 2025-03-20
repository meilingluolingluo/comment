package com.mll.utils;

import com.mll.mapper.ShopMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.mll.entity.Shop;

import java.util.List;

@Component
public class BloomFilterHelper {


    private static final int SIZE = 1 << 24; // 布隆过滤器的位图大小，建议根据实际情况调整
    private static final int[] SEEDS = new int[]{3, 5, 7, 11, 13, 31, 37, 61}; // 布隆过滤器的哈希函数种子

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopMapper shopMapper;

    private RedisBitMap bitMap;

    @PostConstruct
    public void init() {
        bitMap = new RedisBitMap(stringRedisTemplate, "bloom_filter", SIZE);

        List<Long> shopIds = shopMapper.selectList(null)
                .stream()
                .map(Shop::getId)
                .toList();

        for (Long id : shopIds) {
            add(id);
        }
    }

    private void add(Long id) {
        for (int seed : SEEDS) {
            int hash = hash(id, seed);
            bitMap.setBit(hash);
        }
    }

    public boolean mightContain(Long id) {
        for (int seed : SEEDS) {
            int hash = hash(id, seed);
            if (!bitMap.getBit(hash)) {
                return false;
            }
        }
        return true;
    }

    private int hash(Long value, int seed) {
        return (int) ((value ^ seed) % SIZE);
    }

    private static class RedisBitMap {
        private final StringRedisTemplate redisTemplate;
        private final String key;
        private final int size;

        public RedisBitMap(StringRedisTemplate redisTemplate, String key, int size) {
            this.redisTemplate = redisTemplate;
            this.key = key;
            this.size = size;
        }

        public void setBit(int offset) {
            redisTemplate.opsForValue().setBit(key, offset, true);
        }

        public boolean getBit(int offset) {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, offset));
        }
    }
}
