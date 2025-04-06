贡橙优选 - 本地生活服务系统

项目简介

**Comment** 是一个本地生活服务系统，该系统实现了商家查询、优惠券秒杀、附近商户查询及用户签到等功能，重点优化了高并发场景下的性能和数据一致性。

技术栈

- **后端**: Java, Spring Boot
- **数据库**: MySQL
- **缓存**: Redis, Redisson
- **持久层**: MyBatis-Plus
- **其他**: Lua 脚本, Snowflake 分布式 ID 生成器

------

项目功能

1. **短信登录**
   - 发送验证码、验证码登录、登录校验拦截、退出登录
2. **商家查询缓存**
   - 商家信息缓存、首页列表缓存（支持 String、List、ZSet 策略）、缓存穿透/雪崩/击穿解决方案
3. **达人探店**
   - 博客发布、查看、点赞、点赞排行榜
4. **好友关注**
   - 关注/取关、查看他人主页、共同关注、关注推送（Feed 流）
5. **优惠券秒杀**
   - 全局 ID 生成、秒杀下单、一人一单、分布式锁优化（Redisson）、消息队列
6. **附近商户**
   - 基于 Redis GEO 实现的附近商铺查询
7. **用户签到**
   - 基于 BitMap 的签到功能及统计
8. **UV 统计**

------

项目结构

```text
├─java
│  └─com
│      └─mll
│          │  RedisApplication.java           # 项目启动类
│          │
│          ├─config                         # 配置类
│          │      MvcConfig.java
│          │      MybatisConfig.java
│          │      RedissonConfig.java
│          │      SnowflakeConfig.java
│          │      WebExceptionAdvice.java
│          │
│          ├─controller                     # 控制器层
│          │      BlogController.java
│          │      FollowController.java
│          │      ShopController.java
│          │      ShopTypeController.java
│          │      UploadController.java
│          │      UserController.java
│          │      VoucherController.java
│          │      VoucherOrderController.java
│          │
│          ├─dto                           # 数据传输对象
│          │      LoginFormDTO.java
│          │      Result.java
│          │      ScrollResult.java
│          │      UserDTO.java
│          │
│          ├─entity                        # 实体类
│          │      Blog.java
│          │      Follow.java
│          │      SeckillVoucher.java
│          │      Shop.java
│          │      ShopType.java
│          │      User.java
│          │      UserInfo.java
│          │      Voucher.java
│          │      VoucherOrder.java
│          │
│          ├─mapper                        # MyBatis Mapper 接口
│          │      BlogCommentsMapper.java
│          │      BlogMapper.java
│          │      FollowMapper.java
│          │      SeckillVoucherMapper.java
│          │      ShopMapper.java
│          │      ShopTypeMapper.java
│          │      UserInfoMapper.java
│          │      UserMapper.java
│          │      VoucherMapper.java
│          │      VoucherOrderMapper.java
│          │
│          ├─service                       # 服务层接口及实现
│          │  │  IBlogService.java
│          │  │  IFollowService.java
│          │  │  ISeckillVoucherService.java
│          │  │  IShopService.java
│          │  │  IShopTypeService.java
│          │  │  IUserInfoService.java
│          │  │  IUserService.java
│          │  │  IVoucherOrderService.java
│          │  │  IVoucherService.java
│          │  │
│          │  └─impl
│          │          BlogServiceImpl.java
│          │          FollowServiceImpl.java
│          │          SeckillVoucherServiceImpl.java
│          │          ShopServiceImpl.java
│          │          ShopTypeServiceImpl.java
│          │          UserInfoServiceImpl.java
│          │          UserServiceImpl.java
│          │          VoucherOrderServiceImpl.java
│          │          VoucherServiceImpl.java
│          │
│          └─utils                         # 工具类
│                  BloomFilterHelper.java
│                  CacheClient.java
│                  ILock.java
│                  LoginInterceptor.java
│                  RedisConstants.java
│                  RedisData.java
│                  RedisIdGeneratorService.java
│                  RedisLockImpl.java
│                  RedissonLock.java
│                  RefreshTokenInterceptor.java
│                  RegexPatterns.java
│                  RegexUtils.java
│                  SnowflakeIdGenerator.java
│                  SystemConstants.java
│                  UserHolder.java
│
└─resources                                 # 资源文件
    │  application.yaml                     # 配置文件
    │
    ├─lua                                   # Lua 脚本
    │      seckill.lua
    │      unlock.lua
    │
    └─mapper                                # MyBatis XML 文件
            VoucherMapper.xml
```

------

启动流程

前置条件

1. 安装并配置以下环境：
   - JDK 8+
   - MySQL 8.0+
   - Redis 6.0+
   - Maven 3.6+
2. 创建 MySQL 数据库并导入初始表结构（见 docs/database.sql，需自行补充）。
3. 配置 application.yaml 文件，包括数据库连接、Redis 配置等。

启动步骤

1. 克隆项目：

   bash

   ```bash
   git clone https://github.com/meilingluolingluo/comment.git
   ```

2. 进入项目目录：

   bash

   ```bash
   cd comment
   ```

3. 安装依赖：

   bash

   ```bash
   mvn clean install
   ```

4. 运行项目：

   bash

   ```bash
   mvn spring-boot:run
   ```

5. 访问接口：默认地址为 http://localhost:8080。

------

部署

后端部署

1. 打包项目：

   bash

   ```bash
   mvn clean package
   ```

2. 将生成的 target/*.jar 文件上传至服务器。

3. 运行 JAR 文件：

   bash

   ```bash
   java -jar comment-1.0.0.jar
   ```

前端部署

使用 Nginx 部署静态文件，代理后端接口。

------

核心功能实现

1. 短信登录

- **验证码发送**: 通过 Redis 存储验证码，设置过期时间。
- **登录校验**: 使用拦截器验证 Token，刷新 Token 有效期。
- **退出登录**: 删除 Redis 中的 Token。
- 商家查询缓存

- **缓存策略**: 支持 String、List、ZSet 三种数据结构。
- **问题解决**: 缓存穿透（布隆过滤器）、缓存雪崩（随机 TTL）、缓存击穿（互斥锁+逻辑过期）。
- 优惠券秒杀

- **分布式锁**: 使用 Redisson 解决高并发下的超卖问题。
- **消息队列**: 基于 Redis List、PubSub、Stream 实现异步下单。
- 附近商户

- **GEO 实现**: 将店铺经纬度导入 Redis GEO，查询附近商铺。
- 用户签到

- **BitMap**: 使用 Redis BitMap 记录签到状态，统计连续签到天数。

------


贡献

欢迎提交 Issue 或 Pull Request，共同完善项目！

