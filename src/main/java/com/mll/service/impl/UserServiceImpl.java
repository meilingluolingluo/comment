package com.mll.service.impl;

import java.util.Random;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.LoginFormDTO;
import com.mll.dto.Result;
import com.mll.entity.User;
import com.mll.mapper.UserMapper;
import com.mll.service.IUserService;
import com.mll.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 生成随机验证码
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000)); // 生成一个六位数的随机整数

        // 保存验证码到 Session
        session.setAttribute("verifyCode", code);

        // 发送验证码（这里假设发送成功）
        log.debug("发送短信验证码成功，验证码：{}",code);

        // 返回成功结果
        return Result.ok("验证码发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 校验手机号

        //2. 校验验证码

        //3. 若不一致则报错

        //4. 若一则根据手机号查询用户
        return null;
    }
}
