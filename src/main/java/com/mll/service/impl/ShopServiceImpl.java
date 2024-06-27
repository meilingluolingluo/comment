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
import org.springframework.data.geo.Distance;

import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
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

    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            System.out.println("不需要坐标查询，按数据库查询"+page.getRecords());
            return Result.ok(page.getRecords());
        }

        int pageSize = SystemConstants.DEFAULT_PAGE_SIZE;
        int from = (current - 1) * pageSize;
        int end = current * pageSize;

        String key = SHOP_GEO_KEY + typeId;
        System.out.println("需要坐标查询，按距离排序");
        System.out.println("x+y="+x+"y"+y);
        GeoResults<GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key, GeoReference.fromCoordinate(x, y), new Distance(1000),
                       GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));

        if (results == null || results.getContent().isEmpty()) {
            System.out.println("1没有找到满足条件的店铺");
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<GeoLocation<String>>> list = results.getContent();
        if (from > list.size()) {
            System.out.println("2没有找到满足条件的店铺");
            return Result.ok(Collections.emptyList());
        }
        List<String> shopIds = list.stream()
                .map(result -> result.getContent().getName())
                .collect(Collectors.toList());

        List<Shop> shops = query().in("id", shopIds).list();
        System.out.println("找到满足条件的店铺"+shops);

        // 构建距离映射
        Map<String, Distance> distanceMap = list.stream()
                .collect(Collectors.toMap(
                        result -> result.getContent().getName(),
                        GeoResult::getDistance
                ));

        // 排序商铺列表并设置距离
        List<Shop> sortedShops = shopIds.stream()
                .map(shopId -> {
                    Shop shop = shops.stream()
                            .filter(s -> shopId.equals(s.getId().toString()))
                            .findFirst()
                            .orElse(null);
                    if (shop != null) {
                        shop.setDistance(distanceMap.get(shopId).getValue());
                    }
                    return shop;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        System.out.println("sortedshops"+sortedShops);

        return Result.ok(sortedShops);
    }




}
