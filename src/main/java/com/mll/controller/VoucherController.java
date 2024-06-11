package com.mll.controller;

import com.mll.dto.Result;
import com.mll.entity.Voucher;
import com.mll.service.IVoucherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher")
public class                                 VoucherController {
    @Resource
    private IVoucherService voucherService;
    @PostMapping("/seckill")
    public Result addSeckillVoucher(Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        System.out.println("voucher = "+voucher.getId());
        return Result.ok(voucher.getId());
    }
    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }

    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }
}
