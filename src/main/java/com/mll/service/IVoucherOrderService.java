package com.mll.service;

import com.mll.dto.Result;
import com.mll.entity.VoucherOrder;

public interface IVoucherOrderService {
    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
}
