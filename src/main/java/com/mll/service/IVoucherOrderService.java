package com.mll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mll.dto.Result;
import com.mll.entity.VoucherOrder;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
