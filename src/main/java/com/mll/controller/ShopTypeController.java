package com.mll.controller;

import com.mll.dto.Result;
import com.mll.entity.ShopType;
import com.mll.service.IShopTypeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {

    @Resource
    private IShopTypeService typeService;


    @GetMapping("list")
    public Result queryTypeList() {
        List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }

}
