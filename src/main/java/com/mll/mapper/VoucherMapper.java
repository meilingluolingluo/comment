package com.mll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mll.entity.Voucher;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;

import java.util.List;

public interface VoucherMapper extends BaseMapper<Voucher> {
    Result queryVoucherOfShop(@Param("shopId") Long shopId);
    Result addSeckillVoucher(Voucher voucher);

    List<Voucher> selectByShopId(Long shopId);
}
