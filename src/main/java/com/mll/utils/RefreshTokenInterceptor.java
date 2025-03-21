package com.mll.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mll.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mll.utils.RedisConstants.LOGIN_USER_KEY;
import static com.mll.utils.RedisConstants.LOGIN_USER_TTL;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头的token
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            System.out.println("Token is blank");
            return true;
        }

        System.out.println("Token: " + token);

        String key = LOGIN_USER_KEY + token;
        // 2. 获取用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if (userMap.isEmpty()) {
            System.out.println("User not found in Redis for token: " + token);
            return true;
        }

        System.out.println("User found in Redis for token: " + token);

        // 3. 将 Map<Object, Object> 转换为 Map<String, String>
        Map<String, String> stringUserMap = userMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));

        System.out.println("User map: " + stringUserMap);

        // 4. 将 map 转换为 UserDTO 对象
        UserDTO user = BeanUtil.fillBeanWithMap(stringUserMap, new UserDTO(), false);
        System.out.println("UserDTO: " + user);

        // 5. 保存用户信息
        UserHolder.saveUser(user);

        // 6. 刷新token过期时间
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        System.out.println("Refreshed token expiry for token: " + token);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("Removing user from UserHolder");
        UserHolder.removeUser();
    }
}
