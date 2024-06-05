package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.Shop;
import com.mll.mapper.ShopMapper;
import com.mll.service.IShopService;
import com.mll.utils.BloomFilterHelper;
import com.mll.utils.CacheClient;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        return null;
    }


}
