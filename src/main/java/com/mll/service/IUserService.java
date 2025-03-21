package com.mll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mll.dto.LoginFormDTO;
import com.mll.dto.Result;
import com.mll.entity.User;
import jakarta.servlet.http.HttpSession;

public interface IUserService extends IService<User> {
    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();


    Result logout(String token);
}
