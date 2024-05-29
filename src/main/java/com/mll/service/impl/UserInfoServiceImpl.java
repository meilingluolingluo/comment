package com.mll.service.impl;

import com.mll.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.mapper.UserInfoMapper;
import com.mll.service.IUserInfoService;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
}
