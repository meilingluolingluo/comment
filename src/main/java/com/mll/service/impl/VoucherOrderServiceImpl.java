package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.VoucherOrder;
import com.mll.mapper.VoucherOrderMapper;
import com.mll.service.ISeckillVoucherService;
import com.mll.service.IVoucherOrderService;
import com.mll.utils.RedisIdGeneratorService;
import com.mll.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdGeneratorService redisIdGeneratorService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final String SECKILL_SCRIPT_PATH = "lua/seckill.lua";

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource(SECKILL_SCRIPT_PATH));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    // 阻塞队列, 用于存储阻塞的订单
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    private static final ExecutorService SECKILL_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        SECKILL_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 1. 获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    handleVoucherOrder(voucherOrder);
                    System.out.println("处理订单" + voucherOrder.getId() + "成功");

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("用户{}获取锁失败");
            return;
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            proxy.createVoucherOrder(voucherOrder);

        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.singletonList("seckill:stock:" + voucherId),
                userId.toString()
        );

        if (result == null || result != 0) {
            return Result.fail(result == null ? "系统错误" : (result == 1 ? "库存不足" : "不能重复下单"));
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        voucherOrder.setId(new BigInteger(redisIdGeneratorService.generateUniqueId("order")));

        orderTasks.add(voucherOrder);

        return Result.ok(voucherOrder.getId());
    }
//    @Override
//    @Transactional
//    public Result seckillVoucher(Long voucherId) {
//        // 1. 查询秒杀券信息
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        LocalDateTime now = LocalDateTime.now();
//
//        // 2. 校验秒杀时间
//        if (seckillVoucher == null || seckillVoucher.getBeginTime().isAfter(now) || seckillVoucher.getEndTime().isBefore(now)) {
//            return Result.fail(seckillVoucher == null ? "无效的秒杀券" : "秒杀尚未开始或已结束");
//        }
//
//        // 3. 校验库存
//        if (seckillVoucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//        Long userId = UserHolder.getUser().getId();
//        //方法一：自定义分布式锁
//        //RedisLockImpl lock = new RedisLockImpl(stringRedisTemplate,"order:"+ userId);
//        //方法二：使用Redisson实现分布式锁
//        RLock lock = redissonClient.getLock("order:"+ userId);
//        boolean isLock = lock.tryLock();
//        if((!isLock)){
//            return Result.fail("获取锁失败");
//        }
//
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//
//    }

//    @Override
//    @Transactional
//    public Result createVoucherOrder(Long voucherId) {
//        Long userId = UserHolder.getUser().getId();
//
//        // 1. 实现一人一单
//        if (isVoucherAlreadyOrdered(userId, voucherId)) {
//            //System.out.println("一人一单");
//            return Result.fail("购买已达到上限");
//        }
//
//        // 2. 扣减库存并校验库存
//        if (!deductStock(voucherId)) {
//            return Result.fail("库存不足");
//        }
//
//        // 3. 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        voucherOrder.setId(new BigInteger(redisIdGeneratorService.generateUniqueId("order")));
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherId);
//        save(voucherOrder);
//
//        // 4. 返回订单id
//        return Result.ok(voucherOrder.getId());
//    }

    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        if (isVoucherAlreadyOrdered(userId, voucherOrder.getVoucherId())) {
            log.error("一人一单");
            return;
        }
        if (!deductStock(voucherOrder.getVoucherId())) {
            log.error("库存不足");
            return;
        }
        save(voucherOrder);
    }

    private boolean isVoucherAlreadyOrdered(Long userId, Long voucherId) {
        return query().eq("user_id", userId).eq("voucher_id", voucherId).count() > 0;
    }

    private boolean deductStock(Long voucherId) {
        return seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
    }
}
