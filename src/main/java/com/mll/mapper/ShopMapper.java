package com.mll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mll.dto.Result;
import com.mll.entity.Shop;

import java.util.List;

public interface ShopMapper extends BaseMapper<Shop> {
    Result queryById(Long id);
    Result update(Shop shop);
    Result getAllShopIds();
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
