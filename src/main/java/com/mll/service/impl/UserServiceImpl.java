package com.mll.service.impl;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.entity.User;
import com.mll.mapper.UserMapper;
import com.mll.service.IUserService;
import com.mll.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 生成随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 保存验证码到 Session
        session.setAttribute("verifyCode", code);

        // 发送验证码（这里假设发送成功）

        // 返回成功结果
        return Result.ok("验证码发送成功");
    }
}
