package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.entity.SeckillVoucher;
import com.mll.entity.Voucher;
import com.mll.mapper.SeckillVoucherMapper;
import com.mll.service.ISeckillVoucherService;
import org.springframework.stereotype.Service;

@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper,SeckillVoucher> implements ISeckillVoucherService {
    @Override
    public void addSeckillVoucher(Voucher voucher) {
        return;
    }
}
