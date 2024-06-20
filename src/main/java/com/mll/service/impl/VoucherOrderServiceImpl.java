package com.mll.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdGeneratorService redisIdGeneratorService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private IVoucherOrderService proxy;
    private DefaultRedisScript<Long> seckillScript;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String SECKILL_SCRIPT_PATH = "lua/seckill.lua";
    private static final String QUEUE_NAME = "stream.orders";
    private static final String GROUP_NAME = "g1";
    private static final String CONSUMER_NAME = "c1";

    @PostConstruct
    public void init() {
        // 加载秒杀Lua脚本
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource(SECKILL_SCRIPT_PATH));
        seckillScript.setResultType(Long.class);
        // 启动订单处理线程
        executorService.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    initStream();
                    // 获取消息队列的订单信息
                    List<MapRecord<String, Object, Object>> list = getVoucherOrderFromStream(ReadOffset.lastConsumed());
                    if (list == null || list.isEmpty()) continue;

                    MapRecord<String, Object, Object> record = list.getFirst();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(record.getValue(), new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    acknowledgeOrder(record);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void initStream() {
            Boolean exists = stringRedisTemplate.hasKey(QUEUE_NAME);
            if (BooleanUtil.isFalse(exists)) {
                log.info("stream不存在，开始创建stream");
                stringRedisTemplate.opsForStream().createGroup(QUEUE_NAME, ReadOffset.latest(), GROUP_NAME);
                log.info("stream和group创建完毕");
            } else {
                StreamInfo.XInfoGroups groups = stringRedisTemplate.opsForStream().groups(QUEUE_NAME);
                if (groups.isEmpty()) {
                    log.info("group不存在，开始创建group");
                    stringRedisTemplate.opsForStream().createGroup(QUEUE_NAME, ReadOffset.latest(), GROUP_NAME);
                    log.info("group创建完毕");
                }
            }
        }

        private void acknowledgeOrder(MapRecord<String, Object, Object> record) {
            stringRedisTemplate.opsForStream().acknowledge(QUEUE_NAME, GROUP_NAME, record.getId());
        }
    }

    private void handlePendingList() {
        while (true) {
            try {
                List<MapRecord<String, Object, Object>> list = getVoucherOrderFromStream(ReadOffset.from("0"));
                if (list == null || list.isEmpty()) break;

                MapRecord<String, Object, Object> record = list.getFirst();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(record.getValue(), new VoucherOrder(), true);
                handleVoucherOrder(voucherOrder);
                acknowledgeOrder(record);
            } catch (Exception e) {
                log.error("处理挂起订单异常", e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    log.error("线程休眠被中断", interruptedException);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        try {
            proxy.createVoucherOrder(voucherOrder);
        } catch (Exception e) {
            log.error("创建订单失败", e);
        }
    }

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        String orderId = redisIdGeneratorService.generateUniqueId("order");
        Long result = stringRedisTemplate.execute(
                seckillScript,
                Collections.singletonList("seckill:stock:" + voucherId),
                voucherId.toString(), userId.toString(), orderId
        );

        if (result == null || result != 0) {
            return Result.fail(result == null ? "系统错误" : (result == 1 ? "库存不足" : "不能重复下单"));
        }

        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

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
    private List<MapRecord<String, Object, Object>> getVoucherOrderFromStream(ReadOffset readOffset) {
        return stringRedisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                StreamOffset.create(QUEUE_NAME, readOffset)
        );
    }
    private void acknowledgeOrder(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.opsForStream().acknowledge(QUEUE_NAME, GROUP_NAME, record.getId());
    }
}
