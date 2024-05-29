package com.mll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mll.dto.Result;
import com.mll.entity.User;
import jakarta.servlet.http.HttpSession;
import org.apache.ibatis.annotations.Mapper;

public interface UserMapper extends BaseMapper<User> {
    Result sendCode(String phone, HttpSession session);
}
