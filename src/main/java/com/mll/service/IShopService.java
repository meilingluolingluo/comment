package com.mll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mll.dto.Result;
import com.mll.entity.Shop;

import java.util.List;

public interface IShopService extends IService<Shop> {
    Result queryById(Long id);
    Result update(Shop shop);
    Result getAllShopIds();

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
