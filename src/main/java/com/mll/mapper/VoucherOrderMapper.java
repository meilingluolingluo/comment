package com.mll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mll.dto.Result;
import com.mll.entity.Voucher;
import com.mll.entity.VoucherOrder;

public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
    Result queryVoucherOfShop(Long shopId);
    Result addSeckillVoucher(Voucher voucher);

}
