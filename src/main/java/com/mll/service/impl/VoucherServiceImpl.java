package com.mll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.SeckillVoucher;
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

import static com.mll.utils.RedisConstants.SECKILL_STOCK_KEY;

@Slf4j
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VoucherMapper voucherMapper;

    @Override
    public Result queryVoucherOfShop(Long shopId) {

//        QueryWrapper<Voucher> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("shop_id", shopId);
//        // 假设数据库中存储shopId的列名是shop_id
//        List<Voucher> vouchers = voucherMapper.selectList(queryWrapper);

        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
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
        save(voucher);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
        return Result.ok();
    }
}
