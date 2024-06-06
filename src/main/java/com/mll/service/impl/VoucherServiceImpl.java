package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.Voucher;
import com.mll.mapper.VoucherMapper;
import com.mll.service.ISeckillVoucherService;
import com.mll.service.IVoucherService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = baseMapper.selectByShopId(shopId); // Assuming there's a custom query in VoucherMapper
        if (vouchers == null || vouchers.isEmpty()) {
            return Result.fail("No vouchers found for the shop.");
        }
        return Result.ok(vouchers);
    }

    @Transactional
    @Override
    public Result addSeckillVoucher(Voucher voucher) {
        // Assuming some validation logic here
        if (voucher == null) {
            return Result.fail("Invalid voucher.");
        }

        // Save the voucher in the database
        save(voucher);

        // Add seckill voucher logic
        seckillVoucherService.addSeckillVoucher(voucher);

        // Optionally, you can cache the voucher information using Redis
        stringRedisTemplate.opsForValue().set("voucher:" + voucher.getId(), voucher.toString());

        return Result.ok();
    }
}
