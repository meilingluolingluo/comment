package com.mll.controller;

import com.mll.dto.Result;
import com.mll.entity.Voucher;
import com.mll.service.IVoucherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher")
public class VoucherController {
    @Resource
    private IVoucherService voucherService;
    @GetMapping("/id")
    public Result queryVoucherOfShop(Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }
    @PostMapping("/add")
    public Result addSeckillVoucher(Voucher voucher) {
        return voucherService.addSeckillVoucher(voucher);
    }
}
