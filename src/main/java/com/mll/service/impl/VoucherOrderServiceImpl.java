package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.SeckillVoucher;
import com.mll.entity.VoucherOrder;
import com.mll.mapper.VoucherOrderMapper;
import com.mll.service.ISeckillVoucherService;
import com.mll.service.IVoucherOrderService;
import com.mll.utils.RedisIdGeneratorService;
import com.mll.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdGeneratorService redisIdGeneratorService;


    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //1. 查询
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        //2. 是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }

        //3. 是否结束

        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }

        //4. 判断库存是否充足
        if (seckillVoucher.getStock() < 1){
            return Result.fail("库存不足");
        }

        //5. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .eq("stock", seckillVoucher.getStock())
                .update();
        if(!success){
            return Result.fail("库存不足");
        }

        //6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        String id = redisIdGeneratorService.generateUniqueId("order");
        BigInteger Id = new BigInteger(id);
        voucherOrder.setId(Id);
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        //7. 返回订单id
        return Result.ok(voucherOrder.getId());

    }

    @Override
    public void createVoucherOrder(VoucherOrder voucherOrder) {

    }
}
