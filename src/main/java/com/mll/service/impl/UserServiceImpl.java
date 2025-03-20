package com.mll.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mll.utils.UserHolder;
import jakarta.annotation.Resource;
import org.modelmapper.ModelMapper;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.LoginFormDTO;
import com.mll.dto.Result;
import com.mll.dto.UserDTO;
import com.mll.entity.User;
import com.mll.mapper.UserMapper;
import com.mll.service.IUserService;
import com.mll.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.mll.utils.RedisConstants.*;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final SecureRandom secureRandom = new SecureRandom();
    private final ModelMapper modelMapper = new ModelMapper();
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 生成随机验证码
        String code = String.format("%06d", secureRandom.nextInt(1000000)); // 生成一个六位数的随机整数
        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 发送验证码（这里假设发送成功）
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 返回成功结果
        return Result.ok("验证码发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 2. 校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 3. 若不一致则报错
            return Result.fail("验证码错误");
        }

        // 4. 根据手机号查询用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 若不存在则创建新用户再保存
            user = createUserWithPhone(phone);
            save(user); // 保存新用户
        }


        // 6. 将用户信息转换为 UserDTO 并保存到 Redis
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, false, true);
        Map<String, String> stringUserMap = userMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));

        // 7. 将用户信息保存到 Redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, stringUserMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }
    @Override
    public Result sign() {
        // 获取当前登录用户ID
        Long userId = UserHolder.getUser().getId();
        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        // 拼接Redis Key
        String key = String.format("%s%d:%s", USER_SIGN_KEY, userId, now.format(DateTimeFormatter.ofPattern("yyyyMM")));
        // 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth() - 1; // Redis的位图操作下标从0开始
        // 写入Redis位图
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth, true);

        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 获取当前登录用户ID
        Long userId = UserHolder.getUser().getId();
        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        // 拼接Redis Key
        String key = String.format("%s%d:%s", USER_SIGN_KEY, userId, now.format(DateTimeFormatter.ofPattern("yyyyMM")));
        // 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 获取本月截至今天为止的所有签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );

        // 如果没有任何签到记录，返回0
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return Result.ok(0);
        }

        // 获取记录的二进制数
        long num = result.get(0);
        int count = 0;

        // 统计连续签到的天数
        while (num != 0) {
            if ((num & 1) == 0) {
                break;
            }
            count++;
            num >>>= 1;
        }

        return Result.ok(count);
    }

    @Override
    public Result logout(String token) {

        // 检查token是否存在
        if (StrUtil.isBlank(token)) {
            return Result.fail("未登录");
        }

        // 拼接Redis中的键
        String userTokenKey = LOGIN_USER_KEY + token;

        // 删除Redis中的键
        //stringRedisTemplate.delete(tokenKey);
        stringRedisTemplate.delete(userTokenKey);

        // 返回退出成功
        return Result.ok("退出成功");
    }


    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
        return user;
    }
}
