package com.mll.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mll.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 打印所有请求头


        // 1. 获取请求头的token
        String token = request.getHeader("Authorization");

        //System.out.println("token:"+token);
        if (StrUtil.isBlank(token)) {
            return true;
        }
        String key = RedisConstants.LOGIN_USER_KEY + token;

        // 2. 获取用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if (userMap.isEmpty()) {
            return true;
        }

        // 3. 将 Map<Object, Object> 转换为 Map<String, String>
        Map<String, String> stringUserMap = userMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));

        // 4. 将 map 转换为 UserDTO 对象
        UserDTO user = BeanUtil.fillBeanWithMap(stringUserMap, new UserDTO(), false);

        // 5. 保存用户信息
        UserHolder.saveUser(user);

        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
