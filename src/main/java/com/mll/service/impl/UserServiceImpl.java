package com.mll.service.impl;

import java.security.SecureRandom;

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

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
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
        session.setAttribute("code", code);

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
        Object cacheCode = session.getAttribute("code");
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

        //5. 将用户信息转换为UserDTO并保存到Session
      //  UserDTO userDTO = new UserDTO();
        // 手动复制属性
//        userDTO.setId(user.getId());
//        userDTO.setIcon(user.getIcon());
//        userDTO.setNickName(user.getNickName());
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        session.setAttribute("user", userDTO);


        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
        return user;
    }
}
