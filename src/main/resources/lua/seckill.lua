-- 获取传入的参数
local voucherId = ARGV[1]  -- 秒杀商品的ID
local userId = ARGV[2]     -- 用户的ID
local orderId = ARGV[3]    -- 订单的ID

-- 构造 Redis 键
local stockKey = 'seckill:stock:' .. voucherId  -- 库存键
local orderKey = 'seckill:order:' .. voucherId  -- 已下单用户集合键

-- 获取库存值
local stock = redis.call('get', stockKey)

-- 检查库存是否为空
if (stock == false or stock == nil) then
    return 1  -- 库存不足，返回1
end

-- 转换库存为数字类型
stock = tonumber(stock)

-- 检查库存
if (stock <= 0) then
    return 1  -- 库存不足，返回1
end

-- 检查用户是否已下单
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2  -- 用户已下单，返回2
end

-- 扣减库存
redis.call('incrby', stockKey, -1)

-- 将用户添加到已下单集合中
redis.call('sadd', orderKey, userId)
-- 3.6.发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0