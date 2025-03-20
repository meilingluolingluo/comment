package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.Shop;
import com.mll.mapper.ShopMapper;
import com.mll.service.IShopService;
import com.mll.utils.BloomFilterHelper;
import com.mll.utils.CacheClient;
import com.mll.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mll.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ShopMapper shopMapper;
    @Resource
    private BloomFilterHelper bloomFilterHelper;
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id){

        Shop shop = cacheClient
                .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, shopMapper::selectById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null){
           return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        updateById(shop);
        cacheClient.delete(CACHE_SHOP_KEY + id);
        return Result.ok(shop);
    }

    @Override
    public Result getAllShopIds() {
        try {
            List<Long> shopIds = shopMapper.selectList(null)
                    .stream()
                    .map(Shop::getId)
                    .collect(Collectors.toList());
            return Result.ok(shopIds);
        } catch (Exception e) {
            return Result.fail("Failed to fetch shop IDs");
        }
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        return null;

    }


}
