package com.mll.controller;


import com.mll.dto.UserDTO;
import com.mll.entity.UserInfo;
import com.mll.dto.Result;
import com.mll.service.IUserInfoService;
import com.mll.service.IUserService;
import com.mll.dto.LoginFormDTO;
import com.mll.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token){

        return userService.logout(token);
    }

    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("用户未登录");
        }
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }

    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }

    @PutMapping("/info")
    public Result updateInfo(@RequestBody UserInfo userInfo) {
        return userInfoService.updateById(userInfo) ? Result.ok() : Result.fail("更新失败");
    }

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable("id") Long id) {
        return Result.ok(userService.getById(id));
    }
}