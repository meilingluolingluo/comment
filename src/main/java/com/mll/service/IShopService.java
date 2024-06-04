package com.mll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mll.dto.Result;
import com.mll.entity.Shop;
import org.springframework.data.redis.domain.geo.RadiusShape;

import java.util.List;

public interface IShopService extends IService<Shop> {
    Result queryById(Long id);
    Result update(Shop shop);
    List<Long> getAllShopIds();
}
