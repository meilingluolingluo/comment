package com.mll.service.impl;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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

        // 保存验证码到 Session
        //session.setAttribute("code", code);
        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 发送验证码（这里假设发送成功）
        log.debug("发送短信验证码成功，验证码：{}", code);

        // 返回成功结果
        return Result.ok("验证码发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        //2. 校验验证码
        //Object cacheCode = session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        //
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            //3. 若不一致则报错
            return Result.fail("验证码错误");
        }

        //4. 根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //System.out.println(user);

        if (user == null) {
            //若不存在则创建新用户再保存
            user = createUserWithPhone(phone);
            save(user); // 保存新用户
        }

        // 将用户信息转换为UserDTO并保存到Redis
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);


        // 将用户信息转换为Map<String, Object>
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, false, true);

        // 将Map<String, Object>转换为Map<String, String>
        Map<String, String> stringUserMap = userMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));

       // UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        /*
        Map<String, Object> stringUserMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        */

        // 将用户信息保存到Redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, stringUserMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);


        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
        return user;
    }
}
