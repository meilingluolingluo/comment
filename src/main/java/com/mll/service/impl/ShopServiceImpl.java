package com.mll.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.Shop;
import com.mll.mapper.ShopMapper;
import com.mll.service.IShopService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.RandomUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mll.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;
    @Override
    public Result queryById(Long id){

        String cacheKey = CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(cacheKey);
        //商品id存在,直接返回结果
        if (shopJson != null) {
            if (shopJson.isEmpty()) {
                return Result.fail("shop is null");
            }
            return Result.ok(JSONUtil.parseObj(shopJson));
        }

        //redis分布式锁
        String lockKey = LOCK_SHOP_KEY + id;
        try{
            //尝试获取锁
            boolean isLocked = tryLock(lockKey,10);
            //未能获取锁，等待并重试
            if(!isLocked){
                Thread.sleep(100);
                return queryById(id);
            }
            //重复查询缓存：
            //在锁获取后再次查询缓存的逻辑非常重要，可以避免其他线程已经更新了缓存的数据的情况。
            shopJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if(shopJson != null){
                if (shopJson.isEmpty()) {
                    return Result.fail("shop is null");
                }
                return Result.ok(JSONUtil.parseObj(shopJson));
            }
            //若redis中为空，则从数据库中查询
            Shop shop = shopService.getById(id);
            //若sql中也为空，则缓存空结果：以避免缓存穿透
            if(shop == null){
                stringRedisTemplate.opsForValue().set(cacheKey,"",5, TimeUnit.MINUTES);
                return Result.fail("shop is null");
            }
            // 将结果存入缓存，过期时间随机化以避免缓存雪崩
            long randomExpire = LOCK_SHOP_TTL + RandomUtil.randomInt(1,5);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(shop),randomExpire,TimeUnit.MINUTES);
            return Result.ok(shop);
        } catch (InterruptedException e) {
           Thread.currentThread().interrupt();
           return Result.fail("Failed to acquire lock");
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok(shop);
    }

    @Override
    public List<Long> getAllShopIds() {
        return null;
    }

    //获取锁
    private boolean tryLock(String lockKey, int expireTime) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    // 释放锁
    private void releaseLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }


}
