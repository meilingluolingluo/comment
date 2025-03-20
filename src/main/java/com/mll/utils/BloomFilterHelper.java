package com.mll.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.mll.dto.Result;
import com.mll.mapper.ShopMapper;
import com.mll.service.IShopService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import com.mll.entity.Shop;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BloomFilterHelper {

    private BloomFilter<Long> bloomFilter;

    @Resource
    private ShopMapper shopMapper;

    @PostConstruct
    public void init() {
        List<Long> shopIds = shopMapper.selectList(null)
                .stream()
                .map(Shop::getId) // 提取店铺ID
                .collect(Collectors.toList());
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), shopIds.size(), 0.01); // 1%的误判率
        for (Long id : shopIds) {
            bloomFilter.put(id);
        }
    }

    public boolean mightContain(Long id) {
        return bloomFilter.mightContain(id);
    }
}
